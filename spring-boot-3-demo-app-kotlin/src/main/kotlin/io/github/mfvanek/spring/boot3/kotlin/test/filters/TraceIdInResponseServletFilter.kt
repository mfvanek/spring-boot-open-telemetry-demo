package io.github.mfvanek.spring.boot3.kotlin.test.filters

import io.micrometer.tracing.Tracer
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

@Component
class TraceIdInResponseServletFilter(
    private val tracer: Tracer
) : Filter {
    companion object {
        const val TRACE_ID_HEADER_NAME: String = "X-TraceId"
    }

    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
        val currentSpan = tracer.currentSpan()
        if (currentSpan != null) {
            val resp = servletResponse as HttpServletResponse
            resp.addHeader(TRACE_ID_HEADER_NAME, currentSpan.context().traceId())
        }
        filterChain.doFilter(servletRequest, servletResponse)
    }
}
