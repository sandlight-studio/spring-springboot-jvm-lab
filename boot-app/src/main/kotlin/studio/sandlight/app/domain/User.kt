package studio.sandlight.app.domain

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.Instant

@Entity
@Table(name = "users")
class User(
    @field:NotBlank
    var name: String,
    @field:Email
    @Column(unique = true)
    var email: String,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var createdAt: Instant = Instant.now()
}

