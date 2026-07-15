package studio.sandlight.app.web

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT(
    @param:Autowired private val mvc: MockMvc,
) {

    @Test
    fun `POST users creates user and duplicate email returns 409 problem detail`() {
        val email = "it-${System.nanoTime()}@example.com"
        val body = """{"name":"Jane","email":"$email"}"""

        mvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            jsonPath("$.email") { value(email) }
        }

        mvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isConflict() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.detail") { value("email already exists: $email") }
        }
    }

    @Test
    fun `POST users with invalid body returns 422 with field errors`() {
        mvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """{"name":"","email":"not-an-email"}"""
        }.andExpect {
            status { isUnprocessableContent() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.errors") { isArray() }
            jsonPath("$.errors[*].field") { exists() }
        }
    }
}
