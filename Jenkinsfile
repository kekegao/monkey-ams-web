pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                echo '========== Checkout =========='

                git(
                    branch: 'main',
                    url: 'https://github.com/kekegao/monkey-ams-web.git'
                )
            }
        }

        stage('Maven构建') {
            steps {
                echo '========== Maven Build =========='

                sh 'chmod +x mvnw'
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Check JAR') {
            steps {
                echo '========== 检查 Spring Boot JAR =========='

                sh '''
                    echo "========== WORKSPACE =========="
                    pwd

                    echo "========== JAR =========="
                    find "$WORKSPACE" -type f -name "*.jar" -ls
                '''
            }
        }

        stage('Docker Build') {
            steps {
                echo '========== Docker Build =========='

                sh '''
                    cd "$WORKSPACE"

                    echo "========== JAR =========="

                    ls -lh target/*.jar

                    mkdir -p docker-context

                    cp Dockerfile docker-context/
                    cp target/*.jar docker-context/app.jar

                    cd docker-context

                    docker build \
                        -t monkey-ams-web:latest \
                        .
                '''
            }
        }

        stage('Check Docker Image') {
            steps {
                echo '========== 检查 Docker Image =========='

                sh '''
                    docker images | grep monkey-ams-web
                '''
            }
        }

        stage('Deploy') {
            steps {
                echo '========== 部署 monkey-ams-web =========='

                sh '''
                    echo "========== 停止旧容器 =========="

                    docker stop monkey-ams-web || true

                    echo "========== 删除旧容器 =========="

                    docker rm monkey-ams-web || true

                    echo "========== 启动新容器 =========="

                    docker run -d \
                        --name monkey-ams-web \
                        --network ai-network \
                        -p 8080:8081 \
                        --restart unless-stopped \
                        monkey-ams-web:latest

                    echo "========== 容器状态 =========="

                    docker ps \
                        --filter "name=monkey-ams-web"

                    echo "========== 最近日志 =========="

                    docker logs \
                        --tail 100 \
                        monkey-ams-web
                '''
            }
        }
    }

    post {
        success {
            echo '========== 部署成功 =========='
        }

        failure {
            echo '========== 部署失败 =========='
        }
    }
}