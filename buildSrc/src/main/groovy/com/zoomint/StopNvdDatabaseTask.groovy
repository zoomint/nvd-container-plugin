package com.zoomint

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class StopNvdDatabaseTask extends DefaultTask {
	private static final Logger logger = LoggerFactory.getLogger(StopNvdDatabaseTask)

	NvdContainer container = NvdContainer.INSTANCE

	@TaskAction
	void stopDatabase() {
		logger.debug("Issuing NVD Database stop command")
		container.stop()
	}
}
