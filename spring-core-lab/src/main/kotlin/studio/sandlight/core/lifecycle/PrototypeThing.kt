package studio.sandlight.core.lifecycle

import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import java.util.UUID

class PrototypeThing : InitializingBean, DisposableBean {
    val id: UUID = UUID.randomUUID()

    override fun afterPropertiesSet() {
        println("[lifecycle] PrototypeThing($id) initialized")
    }

    override fun destroy() {
        // Note: Spring does not call destroy() for prototypes on context close.
        println("[lifecycle] PrototypeThing($id) destroy() called")
    }
}

