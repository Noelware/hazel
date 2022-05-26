package dev.floofy.hazel.server.plugins

import dev.floofy.utils.kotlin.ifNotNull
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import org.slf4j.MDC

val RequestMdc = createApplicationPlugin("RequestMdcPlugin") {
    onCall { call ->
        MDC.put("request_method", call.request.httpMethod.value)
        MDC.put("request_uri", call.request.uri)
        call.request.userAgent().ifNotNull { MDC.put("user_agent", it) }
    }

    on(ResponseSent) {
        MDC.remove("request_method")
        MDC.remove("request_uri")
        MDC.remove("user_agent")
    }
}
