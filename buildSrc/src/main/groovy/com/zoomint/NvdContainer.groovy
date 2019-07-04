package com.zoomint

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import org.gradle.api.GradleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

import static com.github.dockerjava.api.model.HostConfig.newHostConfig

class NvdContainer {
    public static final NvdContainer INSTANCE = new NvdContainer()

	private static final Logger logger = LoggerFactory.getLogger(NvdContainer.class)
	public static final String NVD_CONTAINER_LABEL_KEY = "nvdContainer"
    public static final String NVD_CONTAINER_SYNC_LOG = "Using NVD DB that was synchronized on"
    public static final String DEPCHECK_DATABASE_NAME = "odc"

	public static final String IMAGE = "nvd-container-gradle:latest"
	public static final int H2_DB_PORT = 9092

	private DockerClient dockerClient


	private NvdContainer() {
		def config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost("tcp://localhost:2375")
				.build()

		this.dockerClient = DockerClientBuilder.getInstance(config).build()
	}

	/**
	 * Start a container and add to it a default shared label NVD_CONTAINER_LABEL_KEY and label passed as a second argument of this method
	 * @param hostPort
	 * @param label
	 * @return
	 */
	protected void start(String dcUser, String dcPass, int hostPort) {
		Map<String, String> labels = new HashMap<>();
		labels.put(NVD_CONTAINER_LABEL_KEY, "true")

		def exposed = ExposedPort.tcp(H2_DB_PORT)
		Ports portBindings = new Ports();
		portBindings.bind(exposed, Ports.Binding.bindPort(hostPort));

		CreateContainerResponse createResponse = dockerClient.createContainerCmd(IMAGE)
				.withExposedPorts(exposed)
				.withHostConfig(
				newHostConfig()
						.withAutoRemove(true)
						.withPortBindings(portBindings))
				.withLabels(labels)
				.exec()


		dockerClient.startContainerCmd(createResponse.getId()).exec()

        waitForContainerStart(dcUser, dcPass, hostPort)

		logger.debug("NVD DB container id: '${createResponse.getId()}' started; bound to port ${hostPort}")
	}

    private void waitForContainerStart(String dcUser, String dcPass, int port) {
        String nvdDbString = "jdbc:h2:tcp://localhost:${port}/${DEPCHECK_DATABASE_NAME}"

        int retries = 30
        boolean success = false

        while (!success && retries > 0) {
            logger.info("Trying to connect to NVD container, port: '${port}'. Retries remaining: '${retries}'")

            try {
                Properties info = new Properties();
                info.put("user", dcUser);
                info.put("password", dcPass);

                // https://stackoverflow.com/questions/44740416/drivermanager-doesnt-see-dependency-in-gradle-custom-plugins-task
                Connection connection = new org.h2.Driver().connect(nvdDbString, info)

                Statement statement = connection.createStatement()
                ResultSet rs2 = statement.executeQuery("SELECT value AS stamp FROM PUBLIC.properties WHERE id='NVD CVE Modified'")
                rs2.next()
                String stamp = rs2.getString("stamp")
                Date syncDate = new Date(Long.valueOf(stamp) * 1000)

                logger.info(NVD_CONTAINER_SYNC_LOG + " '{}'", syncDate)

                success = true
            } catch (Exception ex) {
                logger.debug('Exception thrown:', ex)
                Thread.sleep(2000)

            }
            retries--
        }

        if (!success && retries == 0) {
            throw new GradleException("Cannot connect to NVD database container")
        }
    }

	/**
	 * Kills all NVD containers using their label
	 */
	protected void stop() {
		def containers = dockerClient.listContainersCmd().exec()
		containers.each {
			if (it.labels.get(NVD_CONTAINER_LABEL_KEY)) {
				logger.debug("Killing NVD DB container id: '${it.getId()}'")
				dockerClient.killContainerCmd(it.getId()).exec()
			}
		}
	}
}