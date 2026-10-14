package com.ironfleet.urlshortener

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class annotated with @HiltAndroidApp to enable Hilt DI.
 * This triggers Hilt's code generation and acts as the application-level
 * DI component container.
 */
@HiltAndroidApp
class UrlShortenerApp : Application()
