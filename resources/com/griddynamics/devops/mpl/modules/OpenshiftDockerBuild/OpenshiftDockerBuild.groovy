buildTemplate = '''
apiVersion: v1
kind: Template
metadata:
  name: ${APP_NAME}
objects:
- apiVersion: v1
  kind: BuildConfig
  metadata:
    labels:
      app: ${APP_NAME}
      build: ${APP_NAME}
    name: ${APP_NAME}
  spec:
    output:
      to:
        kind: DockerImage
        name: ${REGISTRY_URL}
      pushSecret:
        name: ${PUSH_SECRET}
    source:
      git:
        ref: ${BRANCH_NAME}
        uri: ${SOURCE_REPOSITORY_URL}
      sourceSecret:
        name: ${SOURCE_SECRET_NAME}
      type: Git
    strategy:
      dockerStrategy:
        type: Docker
        pullSecret:
          name: ${PULL_SECRET}
    resources:
      requests:
        cpu: "500m"
        memory: "1536Mi"
      limits:
        cpu: "1000m"
        memory: "2560Mi"
parameters:
- name: APP_NAME
  displayName: Application Name
  description: Unique application name
  required: true
- name: APP_VERSION
  displayName: Application Version
  description: Unique application version
  required: true
- name: SOURCE_REPOSITORY_URL
  displayName: Source Repository URL
  description: The URL of the repository with your application source code
  required: true
- name: SOURCE_SECRET_NAME
  displayName: Source Secret Name
  description: Secret to be used for connecting to source repo
  value: openshift-git-integration-user
  required: true
- name: BRANCH_NAME
  displayName: SCM Branch Name
  description: The branch to be built
  value: develop
  required: true
- name: PUSH_SECRET
  displayName: Secret for pushing to registry
  description: Secret for pushing to registry
  value: artifactory-token
  required: true
- name: PULL_SECRET
  displayName: Secret for pulling from registry
  description: Secret for pulling from registry
  value: artifactory-token
  required: true
- name: REGISTRY_URL
  displayName: OpenShift Container Registry
  description: URL of the container in Registry (baseUrl/container-name:version)
  required: true
'''


dockerRepo = "docker"	                                                          // docker registry repo name on artifacatory
dockerRegistryBaseUrl = "${dockerhub}/${dockerRepo}/"                           // docker registry address on artifactory
openshiftClientToken = "openshift-token"
imagePullSecret = "artifactory-token"       									                  // pull secret name for docker registry on artifactory
imagePushSecret = "artifactory-token"       									                  // push secret name for docker registry on artifactory
gitCredentialSecret = "git-user"
newImageUrl = "${dockerRegistryBaseUrl}/${CFG.appServiceName}/${CFG.softwareModuleName}:${CFG.appVersion}"
//gitUrl = sh returnStdout: true, script: 'git config --get remote.origin.url'
gitUrl = "https://github.com/git/${CFG.softwareModuleName}.git"

openshiftClient {
    openshift.apply(openshift.process("${buildTemplate}", "-p", "APP_NAME=${CFG.softwareModuleName}", "-p", "APP_VERSION=${CFG.appVersion}", "-p", "SOURCE_REPOSITORY_URL=${gitUrl.trim()}", "-p", "BRANCH_NAME=${env.BRANCH_NAME}", "-p", "PUSH_SECRET=${imagePushSecret}", "-p", "PULL_SECRET=${imagePullSecret}", "-p", "REGISTRY_URL=${newImageUrl}", "-p", "SOURCE_SECRET_NAME=${gitCredentialSecret}"))
    openshift.startBuild("${softwareModuleName}", "--wait", "--follow")
}

def openshiftClient(Closure body) {
    openshift.withCluster('insecure://kubernetes.default.svc') {
        openshift.withCredentials(openshiftClientToken) {
            openshift.withProject("${CFG.softwareModuleName}") {
                body()
            }
        }
    }
}