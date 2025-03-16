package com.hisham.removebg

import org.pytorch.Module

object PTModelInMemoryCache {
    @Volatile
    private var instance: Module? = null

    fun getInstance(): Module {
        return instance ?: synchronized(this) {
            instance ?: Module.load(
                assetFilePath(App.appContext(), "model.pt"),
            ).also { instance = it }
        }
    }
}

