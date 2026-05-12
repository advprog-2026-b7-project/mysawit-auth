## Struktur Auth Service
### Component Diagram For Auth Service
Diagram ini menunjukkan arsitektur keseluruhan dari authentication service. Mencakup tiga controller utama (AuthUserController, AssignmentController, AdminUserController), security components (JwtAuthenticationFilter, JwtTokenProvider, SecurityConfig), service layer, repositories, dan integrasi eksternal seperti Google OAuth dan database PostgreSQL.
![Component Diagram](diagram-asset/Auth%20Service%20Component.png)
### Code Diagram - Domain Entities & Event
Diagram ini memperlihatkan struktur domain model.
![Code Diagram](diagram-asset/Code%20Diagram%20-%20Domain%20Entities%20&%20Event.png)
### Code Diagram - Service Layer
Diagram ini menampilkan implementasi business logic dengan tiga service utama.
![Code Diagram](diagram-asset/Code%20Diagram%20-%20Service%20Layer.png)