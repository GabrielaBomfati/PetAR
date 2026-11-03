}
mediaPlayer = mp
mp.setOnCompletionListener { player ->
    if (mediaPlayer === player) mediaPlayer = null
    player.release()
}
mp.start()
} catch (e: Exception) {
    Log.e("CameraFragment", "Erro ao reproduzir som", e)
}
}

private fun modelLocation(fileName: String): String? {
    val context = context ?: return null

    try {
        context.assets.open(fileName).use { }
        return fileName
    } catch (_: Exception) {
        // Procura no armazenamento interno.
    }

    val file = File(context.filesDir, fileName)

    return if (file.isFile) {
        Uri.fromFile(file).toString()
    } else {
        null
    }
}

private fun removeModel() {
    val scene = sceneView

    modelNode?.let { model ->
        runCatching {
            anchorNode?.removeChildNode(model)
            scene?.removeChildNode(model)
            model.destroy()
        }
    }

    modelNode = null

    anchorNode?.let { anchor ->
        runCatching {
            scene?.removeChildNode(anchor)
            anchor.anchor.detach()
            anchor.destroy()
        }
    }

    anchorNode = null
    placementHit = null

    if (mode == Mode.AR) {
        placeButton?.text = "Posicionar"
    }
}

private fun clearScene() {
    generation++
    modelGeneration++

    arWatchdogJob?.cancel()
    arWatchdogJob = null

    cameraWatchdogJob?.cancel()
    cameraWatchdogJob = null

    loadJob?.cancel()
    loadJob = null

    cameraPreview?.let { preview ->
        runCatching {
            cameraProvider?.unbind(preview)
        }
    }

    cameraPreview = null
    cameraProvider = null

    arTracking = false
    cameraStreaming = false

    removeModel()

    sceneView?.let { scene ->
        runCatching {
            scene.lifecycle = null
            scene.destroy()
        }
    }

    sceneView = null
    arView = null

    container?.removeAllViews()
}

private fun fullSize() = FrameLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.MATCH_PARENT
)

private fun dp(value: Int): Int {
    return (value * resources.displayMetrics.density).toInt()
}

override fun onDestroyView() {
    availabilityJob?.cancel()
    availabilityJob = null

    clearScene()

    overlay?.let {
        (it.parent as? ViewGroup)?.removeView(it)
    }

    overlay = null
    statusText = null
    placeButton = null
    btnChangeModel = null
    mediaPlayer?.release()
    mediaPlayer = null
    container = null

    pendingAr = false