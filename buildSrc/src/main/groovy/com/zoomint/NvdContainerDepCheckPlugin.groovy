package com.zoomint

import org.gradle.api.Plugin
import org.gradle.api.Project

class NvdContainerDepCheckPlugin implements Plugin<Project> {
	def final DCUSER = 'dcuser'
	def final DCPASS = 'DC-Pass1337!'

	void apply(Project project) {
		def final hostPort = getAvailablePort()

		def startContainer = project.tasks.register("startContainer", StartNvdDatabaseTask, DCUSER, DCPASS, hostPort)
		def stopContainer = project.tasks.register("stopContainer", StopNvdDatabaseTask)

		project.apply plugin: 'org.owasp.dependencycheck'

		project.dependencyCheck {
			autoUpdate = false // do not update the database, its pre-built
			data {
				connectionString = "jdbc:h2:tcp://localhost:${hostPort}/odc"
				driver = 'org.h2.Driver'
				username = DCUSER
				password = DCPASS
			}
		}

		project.dependencyCheckAnalyze.dependsOn startContainer
		project.dependencyCheckAnalyze.finalizedBy stopContainer
		project.build.dependsOn project.dependencyCheckAnalyze
	}

	static final int getAvailablePort() {
		ServerSocket socket = null
		try {
			socket = new ServerSocket(0)
			return socket.localPort
		} finally {
			if (socket != null) {
				socket.close()
			}
		}
	}
}
