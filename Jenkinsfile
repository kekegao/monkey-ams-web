pipeline {

    agent any

    parameters {

        choice(
            name: 'ACTION',
            choices: [
                'DEPLOY',
                'ROLLBACK'
            ],
            description: '选择执行发布还是回滚'
        )

        string(
            name: 'ROLLBACK_VERSION',
            defaultValue: '',
            description: '回滚版本，例如：1.0.25。ACTION=ROLLBACK 时填写'
        )
    }

    environment {

        // =========================
        // 基础配置
        // =========================


        //APP_PARENT_NAME = 'acq-bsm-biz'
        APP_NAME = 'monkey-ams-web'
        IMAGE_NAME = 'monkey-ams-web'
        CONTAINER_NAME = 'monkey-ams-web'
        DOCKER_NETWORK = 'ai-network'
        HOST_PORT = '8082'
        CONTAINER_PORT = '8081'



        // Spring Boot Actuator
        HEALTH_PATH = '/actuator/health'

        // Docker Hub / Registry 暂时不使用
        // 如果以后使用 Harbor，可以在这里扩展


        // =========================
        // Jenkins 自动版本
        // =========================

        VERSION = "1.0.${BUILD_NUMBER}"


        // 当前正在运行的旧版本
        PREVIOUS_IMAGE = ""

        IMAGE_FULL_NAME = ""

        // 是否已经停止旧容器
        OLD_CONTAINER_STOPPED = 'false'
    }


    stages {


        // ==================================================
        // 1. 判断执行模式
        // ==================================================

        stage('Check Action') {

            steps {

                script {

                    echo "========================================"
                    echo "ACTION = ${params.ACTION}"
                    echo "========================================"

                    if (params.ACTION == 'ROLLBACK') {

                        if (!params.ROLLBACK_VERSION?.trim()) {

                            error """
                            回滚版本不能为空！

                            例如：
                            ROLLBACK_VERSION=1.0.25
                            """
                        }

                        echo "准备回滚到版本：${params.ROLLBACK_VERSION}"

                    } else {

                        echo "准备发布新版本：${env.VERSION}"
                    }
                }
            }
        }


        // ==================================================
        // 2. Git Checkout
        // ==================================================

        stage('Checkout') {

            when {

                expression {
                    params.ACTION == 'DEPLOY'
                }
            }

            steps {

                checkout([
                    $class: 'GitSCM',

                    branches: [[
                        name: '*/dev'
                    ]],

                    userRemoteConfigs: [[
                        url: 'https://github.com/kekegao/monkey-ams-web.git'
                    ]]
                ])
            }
        }


        // ==================================================
        // 3. 获取 Git Commit
        // ==================================================

        stage('Generate Version') {

            when {

                expression {
                    params.ACTION == 'DEPLOY'
                }
            }

            steps {

                script {

                    /* def gitCommitShort = sh(
                        script: 'git rev-parse --short=7 HEAD',
                        returnStdout: true
                    ).trim() */


                    IMAGE_FULL_NAME = "${env.IMAGE_NAME}:${env.VERSION}"


                    echo "========================================"
                    echo "VERSION          = ${env.VERSION}"
                    echo "IMAGE_FULL_NAME  = ${IMAGE_FULL_NAME}"
                    echo "========================================"
                }
            }
        }


        // ==================================================
        // 4. Maven Build
        // ==================================================

        stage('Maven Build') {

            when {

                expression {
                    params.ACTION == 'DEPLOY'
                }
            }

            steps {

                echo "开始 Maven 编译..."

                sh """
                    docker run --rm \
                    -v jenkins_jenkins_home:/var/jenkins_home \
                    -v jenkins-maven-repo:/root/.m2 \
                    -w /var/jenkins_home/workspace/monkey-ams-web \
                    maven:3.9.11-eclipse-temurin-21 \
                    mvn clean package -DskipTests
                """
            }
        }


        // ==================================================
        // 5. Docker Build
        // ==================================================

        stage('Docker Build') {

            when {

                expression {
                    params.ACTION == 'DEPLOY'
                }
            }

            steps {

                echo "开始构建 Docker 镜像..."

                sh """
                    cd "$WORKSPACE"

                    echo "========== JAR =========="

                    ls -lh target/*.jar

                    mkdir -p docker-context

                    cp Dockerfile docker-context/
                    cp target/${APP_NAME}*.jar docker-context/app.jar

                    cd docker-context

                    docker build \
                        -t ${IMAGE_NAME}:${VERSION} \
                        -t ${IMAGE_NAME}:latest \
                        .
                """

                /* sh """
                    docker build \
                        -t ${IMAGE_NAME}:${VERSION} \
                        -t ${IMAGE_NAME}:latest \
                        .
                """ */

                echo "Docker 镜像构建成功"

                sh """
                    docker images ${IMAGE_NAME}
                """
            }
        }


        // ==================================================
        // 6. 获取当前运行版本
        // ==================================================

        stage('Get Current Version') {

            when {

                expression {
                    params.ACTION == 'DEPLOY'
                }
            }

            steps {

                script {

                    def containerExists = sh(
                        script: """
                            docker ps -a \
                            --filter "name=^/${CONTAINER_NAME}\$" \
                            --format "{{.Names}}"
                        """,
                        returnStdout: true
                    ).trim()

                    if (containerExists) {

                        PREVIOUS_IMAGE = sh(
                            script: """
                                docker inspect \
                                --format='{{.Config.Image}}' \
                                ${CONTAINER_NAME}
                            """,
                            returnStdout: true
                        ).trim()

                        echo "当前运行版本：${PREVIOUS_IMAGE}"

                    } else {

                        PREVIOUS_IMAGE = ''

                        echo "当前不存在旧容器，这是第一次部署"
                    }
                }
            }
        }


        // ==================================================
        // 7. 部署
        // ==================================================

        stage('Deploy') {

            when {

                expression {
                    params.ACTION == 'DEPLOY'
                }
            }

            steps {

                script {

                    echo "========================================"
                    echo "开始部署"
                    echo "新版本：${IMAGE_FULL_NAME}"
                    echo "旧版本：${PREVIOUS_IMAGE}"
                    echo "========================================"


                    // 删除旧容器
                    sh """
                        docker rm -f ${CONTAINER_NAME} || true
                    """

                    env.OLD_CONTAINER_STOPPED = 'true'


                    // 启动新容器
                    sh """
                        docker run -d \
                            --name ${CONTAINER_NAME} \
                            --network ${DOCKER_NETWORK} \
                            -p ${HOST_PORT}:${CONTAINER_PORT} \
                            --restart unless-stopped \
                            ${IMAGE_FULL_NAME}
                    """


                    echo "新容器启动完成"

                    sh """
                        docker ps \
                            --filter "name=${CONTAINER_NAME}"
                    """
                }
            }
        }


        // ==================================================
        // 8. 健康检查
        // ==================================================

        stage('Health Check') {

            when {

                expression {
                    params.ACTION == 'DEPLOY'
                }
            }

            steps {

                script {

                    echo "开始健康检查..."

                    def maxRetry = 30

                    def success = false


                    for (int i = 1; i <= maxRetry; i++) {

                        echo "健康检查 ${i}/${maxRetry}"


                        def result = sh(
                            script: """
                                docker run --rm \
                                    --network ${DOCKER_NETWORK} \
                                    curlimages/curl:8.10.1 \
                                    -fsS \
                                    http://${CONTAINER_NAME}:${CONTAINER_PORT}${HEALTH_PATH}
                            """,
                            returnStatus: true
                        )


                        if (result == 0) {

                            echo "========================================"
                            echo "健康检查成功"
                            echo "========================================"

                            success = true

                            break

                        } else {

                            echo "应用还没有准备好，等待 3 秒..."

                            sleep 3
                        }
                    }


                    if (!success) {

                        error """
                        ========================================
                        健康检查失败
                        ========================================
                        应用版本：${IMAGE_FULL_NAME}

                        Jenkins 将自动执行回滚。
                        """
                    }
                }
            }
        }


        // ==================================================
        // 9. 部署成功
        // ==================================================

        stage('Deployment Success') {

            when {

                expression {
                    params.ACTION == 'DEPLOY'
                }
            }

            steps {

                script {

                    echo """
                    ========================================
                    🚀 部署成功
                    ========================================

                    应用：
                    ${APP_NAME}

                    Docker：
                    ${IMAGE_FULL_NAME}

                    Previous：
                    ${PREVIOUS_IMAGE}

                    ========================================
                    """
                }
            }
        }


        // ==================================================
        // 10. 回滚版本检查
        // ==================================================

        stage('Check Rollback Image') {

            when {

                expression {
                    params.ACTION == 'ROLLBACK'
                }
            }

            steps {

                script {

                    echo "检查回滚镜像：${params.ROLLBACK_VERSION}"


                    def imageExists = sh(
                        script: """
                            docker image inspect \
                            ${IMAGE_NAME}:${params.ROLLBACK_VERSION}
                            > /dev/null 2>&1
                        """,
                        returnStatus: true
                    )


                    if (imageExists != 0) {

                        error """
                        ========================================
                        回滚失败
                        ========================================

                        Docker 镜像不存在：

                        ${IMAGE_NAME}:${params.ROLLBACK_VERSION}

                        当前 Docker 镜像：

                        """

                    }


                    echo "回滚镜像存在，可以执行回滚"
                }
            }
        }


        // ==================================================
        // 11. 手动回滚
        // ==================================================

        stage('Rollback') {

            when {

                expression {
                    params.ACTION == 'ROLLBACK'
                }
            }

            steps {

                script {

                    def rollbackImage =
                        "${IMAGE_NAME}:${params.ROLLBACK_VERSION}"


                    echo """
                    ========================================
                    开始回滚
                    ========================================

                    回滚目标：
                    ${rollbackImage}

                    ========================================
                    """


                    // 删除当前容器
                    sh """
                        docker rm -f ${CONTAINER_NAME} || true
                    """


                    // 启动历史版本
                    sh """
                        docker run -d \
                            --name ${CONTAINER_NAME} \
                            --network ${DOCKER_NETWORK} \
                            -p ${HOST_PORT}:${CONTAINER_PORT} \
                            --restart unless-stopped \
                            ${rollbackImage}
                    """


                    echo "回滚容器启动完成"


                    // 健康检查
                    def success = false


                    for (int i = 1; i <= 30; i++) {

                        echo "回滚健康检查 ${i}/30"


                        def result = sh(
                            script: """
                                docker run --rm \
                                    --network ${DOCKER_NETWORK} \
                                    curlimages/curl:8.10.1 \
                                    -fsS \
                                    http://${CONTAINER_NAME}:${CONTAINER_PORT}${HEALTH_PATH}
                            """,
                            returnStatus: true
                        )


                        if (result == 0) {

                            success = true

                            echo "回滚健康检查成功"

                            break

                        }


                        sleep 3
                    }


                    if (!success) {

                        error """
                        ========================================
                        回滚失败！
                        ========================================

                        ${rollbackImage}

                        启动后健康检查仍然失败。
                        """
                    }


                    echo """
                    ========================================
                    🔄 回滚成功
                    ========================================

                    当前版本：
                    ${rollbackImage}

                    ========================================
                    """
                }
            }
        }
    }


    // ==================================================
    // Pipeline Post
    // ==================================================

    post {


        // ==================================================
        // 成功
        // ==================================================

        success {

            echo """
            ========================================
            Jenkins Pipeline SUCCESS
            ========================================
            """
        }


        // ==================================================
        // 自动回滚
        // ==================================================

        failure {

            script {

                if (
                    params.ACTION == 'DEPLOY' &&
                    env.OLD_CONTAINER_STOPPED == 'true' &&
                    env.PREVIOUS_IMAGE?.trim()
                ) {

                    echo """
                    ========================================
                    ⚠️ 新版本部署失败
                    ========================================

                    开始自动回滚：

                    ${PREVIOUS_IMAGE}

                    ========================================
                    """


                    try {

                        // 删除失败的新版本
                        sh """
                            docker rm -f ${CONTAINER_NAME} || true
                        """


                        // 启动旧版本
                        sh """
                            docker run -d \
                                --name ${CONTAINER_NAME} \
                                --network ${DOCKER_NETWORK} \
                                -p 8088:8080 \
                                --restart unless-stopped \
                                ${PREVIOUS_IMAGE}
                        """


                        echo "旧版本启动成功"


                        // 回滚健康检查
                        def rollbackSuccess = false


                        for (int i = 1; i <= 30; i++) {

                            echo "自动回滚健康检查 ${i}/30"


                            def result = sh(
                                script: """
                                    docker run --rm \
                                        --network ${DOCKER_NETWORK} \
                                        curlimages/curl:8.10.1 \
                                        -fsS \
                                        http://${CONTAINER_NAME}:${CONTAINER_PORT}${HEALTH_PATH}
                                """,
                                returnStatus: true
                            )


                            if (result == 0) {

                                rollbackSuccess = true

                                break
                            }


                            sleep 3
                        }


                        if (rollbackSuccess) {

                            echo """
                            ========================================
                            🔄 自动回滚成功
                            ========================================

                            恢复版本：

                            ${PREVIOUS_IMAGE}

                            ========================================
                            """

                        } else {

                            echo """
                            ========================================
                            ❌ 自动回滚失败

                            恢复版本：

                            ${PREVIOUS_IMAGE}
                            ========================================
                            """
                        }


                    } catch (Exception e) {

                        echo """
                        ========================================
                        ❌ 自动回滚过程中发生异常
                        ========================================

                        ${e.getMessage()}

                        ========================================
                        """
                    }


                } else {

                    echo """
                    ========================================
                    Pipeline 执行失败

                    没有执行自动回滚。

                    可能原因：
                    1. 第一次部署
                    2. 旧容器不存在
                    3. 没有获取到旧版本
                    4. 回滚条件不满足

                    ========================================
                    """
                }
            }
        }


        // ==================================================
        // Always
        // ==================================================

        always {

            echo "Jenkins Pipeline 执行结束"

            sh """
                docker ps -a \
                    --filter "name=${CONTAINER_NAME}" \
                    || true
            """
        }
    }
}