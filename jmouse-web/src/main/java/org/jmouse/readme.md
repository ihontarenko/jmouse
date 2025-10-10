| Step | Filter / Component                       | Responsibility                                                                                                                          |
| ---- | ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| 1    | **SecurityContextPersistence**           | Load/Save `SecurityContext` from session or repository                                                                                  |
| 2    | **DefaultLoginPageFilter** *(optional)*  | Render default login form at **GET `/login`** if no custom page exists. Submits form to **POST `/login/process`**                       |
| 3    | **Authentication Filters**               | Build pre-auth token and call `AuthenticationManager.authenticate(...)`                                                                 |
|      | – FormLogin (`POST /login/process`)      | Success → `SuccessHandler` <br> Failure → `FailureHandler` <br> Chain stops on success                                                  |
|      | – BasicAuth (`Authorization: Basic ...`) | Continues chain after authentication                                                                                                    |
|      | – JWT (`Authorization: Bearer ...`)      | Continues chain after authentication                                                                                                    |
| 4    | **AnonymousAuthentication**              | Inject anonymous token if still unauthenticated                                                                                         |
| 5    | **ExceptionTranslation**                 | Catch `AuthenticationException` / `AccessDeniedException` <br> Save request to `RequestCache` <br> Delegate to entry points or handlers |
|      | – AuthenticationEntryPoint               | Redirect unauthenticated users → `/login`                                                                                               |
|      | – AccessDeniedHandler                    | Return **403 Forbidden** (or custom error page)                                                                                         |
| 6    | **AuthorizationFilter**                  | Use `AuthorizationManager`: <br> Needs auth → exception <br> No rights → exception <br> Else → continue                                 |
| 7    | **Dispatcher / Controllers**             | Route to application controllers                                                                                                        |


| Endpoint / Hook              | Behavior / Implementation Example                                     |
| ---------------------------- | --------------------------------------------------------------------- |
| **GET `/login`**             | Your controller/template OR `DefaultLoginPageFilter`                  |
| **POST `/login/process`**    | Handled by FormLogin (`SubmitFormRequestAuthenticationFilter`)        |
| **SuccessHandler**           | `SavedRequestAwareAuthenticationSuccessHandler` (uses `RequestCache`) |
| **FailureHandler**           | `FailureRedirectHandler("/login?error")` or your custom handler       |
| **AuthenticationEntryPoint** | `LoginUrlAuthenticationEntryPoint("/login")` → redirects to `/login`  |
| **AccessDeniedHandler**      | Return 403 Forbidden (or show a custom error page)                    |
| **RequestCache**             | Typically `HttpSessionRequestCache`, shared across filters            |
