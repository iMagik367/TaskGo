package com.taskgoapp.taskgo.core.design

import com.canhub.cropper.CropImageOptions

/**
 * Configurações padrão para o corte de imagem
 */
object CropImageConfig {
    
    /**
     * Cria as opções padrão para corte de imagem
     */
    fun createDefaultOptions(): CropImageOptions {
        return CropImageOptions()
    }
    
    /**
     * Cria opções para corte de imagem com proporção fixa
     */
    fun createFixedAspectRatioOptions(
        aspectRatioX: Int = 1,
        aspectRatioY: Int = 1
    ): CropImageOptions {
        return CropImageOptions()
    }
    
    /**
     * Cria opções para corte de imagem quadrada
     */
    fun createSquareOptions(): CropImageOptions {
        return CropImageOptions()
    }
    
    /**
     * Cria opções para corte de imagem retangular (16:9)
     */
    fun createWideOptions(): CropImageOptions {
        return CropImageOptions()
    }
}

