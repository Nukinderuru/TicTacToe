package nukinderuru.di

import nukinderuru.datasource.repository.CurrentGameRepository
import nukinderuru.datasource.repository.ExposedCurrentGameRepository
import nukinderuru.datasource.repository.ExposedUserRepository
import nukinderuru.datasource.repository.UserRepository
import nukinderuru.domain.service.AuthService
import nukinderuru.domain.service.CurrentGameService
import nukinderuru.domain.service.DefaultUserService
import nukinderuru.domain.service.GameService
import nukinderuru.domain.service.JwtAuthService
import nukinderuru.domain.service.JwtProvider
import nukinderuru.domain.service.UserService
import org.koin.dsl.module

val applicationModule = module {
    single<CurrentGameRepository> { ExposedCurrentGameRepository() }
    single<UserRepository> { ExposedUserRepository() }
    single<UserService> { DefaultUserService(get()) }
    single { JwtProvider() }
    single<AuthService> { JwtAuthService(get(), get()) }
    single<GameService> { CurrentGameService(get()) }
}
