package com.zoomint


import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class StartNvdDatabaseTask extends DefaultTask {
	String dcUser
	String dcPass
	int port

	NvdContainer container = NvdContainer.INSTANCE

	@Inject
	StartNvdDatabaseTask(String dcUser, String dcPass, int port) {
		this.dcUser = dcUser
		this.dcPass = dcPass
		this.port = port
	}

	@TaskAction
	void startDatabase() {
		project.logger.debug 'Starting NVD database'
		container.start(dcUser, dcPass, port)
	}
}
