package com.peakda.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PeakdaServerApplication

fun main(args: Array<String>) {
	runApplication<PeakdaServerApplication>(*args)
}
