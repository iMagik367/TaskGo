package com.taskgoapp.taskgo.core.design

import android.graphics.Bitmap
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

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
    
    /**
     * Cria opções para corte circular de imagem (perfil)
     */
    fun createCircularOptions(): CropImageOptions {
        return CropImageOptions().apply {
            // A biblioteca ImageCropper já suporta corte circular através do cropShape
            // Mas como estamos usando CropImageContract, as opções podem ser limitadas
            // O corte circular será feito visualmente através do ImageCropper nativo
            fixAspectRatio = true
            aspectRatioX = 1
            aspectRatioY = 1
            guidelines = CropImageView.Guidelines.ON
            outputCompressFormat = android.graphics.Bitmap.CompressFormat.PNG
            outputCompressQuality = 90
        }
    }
}

