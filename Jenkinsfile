// Builds and archives the Credence SDK sample application.
//
// The app has no product flavors and no signing configuration, so release
// builds would be unsigned. Only the debug APK is published - that is what is
// shared for on-device testing.

pipeline {
    agent any

    options {
        skipDefaultCheckout true
        timeout(time: 1, unit: 'HOURS')
    }

    environment {
        JAVA_HOME    = '/usr/lib/jvm/java-17-openjdk-amd64'
        ANDROID_HOME = '/opt/android-sdk-linux'
    }

    stages {

        stage('Cleanup Workspace') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        stage('Setup') {
            steps {
                sh 'chmod +x gradlew'
            }
        }

        stage('Set Build Name') {
            steps {
                script {
                    def gradleFile = readFile './app/build.gradle'
                    def match = (gradleFile =~ /versionName\s+"([^"]+)"/)
                    if (!match.find()) {
                        error 'versionName not found in app/build.gradle'
                    }
                    def versionName = match.group(1)

                    env.BUILD_NAME = (env.BRANCH_NAME in ['master', 'development', 'release'])
                        ? versionName
                        : "Build-${versionName}-${env.BUILD_NUMBER}"
                    echo "Branch=${env.BRANCH_NAME}  version=${versionName}  buildName=${env.BUILD_NAME}"
                }
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean assembleDebug'
            }
        }

        stage('Rename & Archive') {
            steps {
                // The app bundles the SDK library by hand from app/libs, so the
                // APK is only as current as whatever AAR is committed there.
                sh '''
                    set -eu
                    mkdir -p artifacts

                    apk=$(find app/build/outputs/apk/debug -name '*.apk' | head -1)
                    test -n "$apk" || { echo "no debug APK produced"; exit 1; }
                    cp "$apk" "artifacts/C-SampleApp-${BUILD_NAME}-debug.apk"

                    echo "bundled SDK library:"
                    ls -1 app/libs/*.aar app/libs/*.jar 2>/dev/null || echo "  none"

                    ls -l artifacts
                '''
                archiveArtifacts artifacts: 'artifacts/*.apk', fingerprint: true, onlyIfSuccessful: true
            }
        }
    }

    post {
        always {
            script {
                if (env.BUILD_NAME) {
                    currentBuild.displayName = env.BUILD_NAME
                }
            }
        }
    }
}
