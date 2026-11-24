package com.ifpr.androidapptemplatear.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.location.LocationServices
import com.ifpr.androidapptemplatear.R
import com.ifpr.androidapptemplatear.databinding.FragmentChangeobjectBinding
import kotlin.random.Random

class ChangeObjectFragment : Fragment() {

    private var binding: FragmentChangeobjectBinding? = null
    private val sharedViewModel: SharedModelViewModel by activityViewModels()
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideCaptureToast = Runnable { binding?.captureToast?.isVisible = false }
    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (sharedViewModel.mapRound != null) return@registerForActivityResult
        if (permissions.values.any { it }) obterLocalizacaoECarregarMapa()
        else iniciarPartida(-23.5505, -46.6333)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_changeobject,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val views = FragmentChangeobjectBinding.bind(view)
        binding = views

        // Aplicar backgrounds arredondados programaticamente
        aplicarBackgroundArredondado(views.scorePanel, Color.parseColor("#CC1B1B2F"))
        aplicarBackgroundArredondado(views.captureToast, Color.parseColor("#CC2E7D32"))
        aplicarBackgroundArredondado(views.encounterCard, Color.parseColor("#1B1B2F"))

        configurarWebView(views.mapWebView)
        views.encounterAction.setOnClickListener {
            if (!views.encounterAction.isEnabled) return@setOnClickListener
            val round = sharedViewModel.mapRound ?: return@setOnClickListener
            if (round.escapedPet != null) {
                iniciarPartida(round.latitude, round.longitude)
            } else {
                lancarPetisco(views, round)
            }
        }

        val round = sharedViewModel.mapRound
        if (round == null) {
            verificarPermissaoLocalizacao()
        } else {
            if (round.escapedPet == null) carregarMapa(views.mapWebView, round)
            atualizarTela(views, round)
        }
    }

    /**
     * Cria um background arredondado programaticamente
     * para evitar criar novos arquivos drawable.
     */
    private fun aplicarBackgroundArredondado(view: View, cor: Int) {
        val drawable = GradientDrawable().apply {
            setColor(cor)
            cornerRadius = 24f
        }
        view.background = drawable
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configurarWebView(mapWebView: WebView) {
        mapWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        mapWebView.webViewClient = WebViewClient()
        mapWebView.webChromeClient = WebChromeClient()

        // O mapa informa qual marcador o jogador tocou.
        mapWebView.addJavascriptInterface(
            MapJsInterface(mapWebView),
            "AndroidPetAR"
        )
    }

    private fun verificarPermissaoLocalizacao() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (permissions.any {
                ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            obterLocalizacaoECarregarMapa()
        } else {
            locationPermission.launch(permissions)
        }
    }

    @SuppressLint("MissingPermission")
    private fun obterLocalizacaoECarregarMapa() {
        val views = binding ?: return
        LocationServices.getFusedLocationProviderClient(requireActivity()).lastLocation
            .addOnCompleteListener { task ->
                if (binding !== views || sharedViewModel.mapRound != null) return@addOnCompleteListener
                val location = if (task.isSuccessful) task.result else null
                iniciarPartida(location?.latitude ?: -23.5505, location?.longitude ?: -46.6333)
            }
    }

    private fun iniciarPartida(lat: Double, lng: Double) {
        val views = binding ?: return
        val round = MapRound(lat, lng)
        sharedViewModel.mapRound = round
        carregarMapa(views.mapWebView, round)
        atualizarTela(views, round)
    }

    private fun carregarMapa(mapWebView: WebView, round: MapRound) {
        val lat = round.latitude
        val lng = round.longitude
        val markersJs = round.pets.joinToString("\n") { pet ->
            "addPet(${pet.id}, ${pet.latitude}, ${pet.longitude}, '${pet.type.emoji}', '${pet.type.color}');"
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    * { margin: 0; padding: 0; }
                    html, body, #map { width: 100%; height: 100%; }
                    .pet-marker { background: none !important; border: none !important; }
                    .leaflet-popup-content-wrapper {
                        border-radius: 12px;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.3);
                    }
                    @keyframes pulse {
                        0% { transform: scale(1); }
                        50% { transform: scale(1.2); }
                        100% { transform: scale(1); }
                    }
                    .pet-marker div {
                        animation: pulse 2s infinite;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map').setView([$lat, $lng], 16);
                    
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '© OpenStreetMap',
                        maxZoom: 19
                    }).addTo(map);
                    
                    // Marcador do usuário
                    var userIcon = L.divIcon({
                        html: '<div style="font-size:28px;text-align:center;filter:drop-shadow(2px 2px 4px rgba(0,0,0,0.5));">📍</div>',
                        className: 'pet-marker',
                        iconSize: [36, 36],
                        iconAnchor: [18, 18]
                    });
                    L.marker([$lat, $lng], {icon: userIcon}).addTo(map)
                        .bindPopup('<b>Você está aqui!</b>');
                    
                    // Círculo de alcance
                    L.circle([$lat, $lng], {
                        radius: 300,
                        color: '#6200EE',
                        fillColor: '#BB86FC',
                        fillOpacity: 0.1,
                        weight: 2,
                        dashArray: '5, 10'
                    }).addTo(map);
                    
                    // Marcadores de pets
                    const pets = new Map();
                    const roundId = '${round.id}';

                    function addPet(id, lat, lng, emoji, color) {
                        const icon = L.divIcon({
                            html: '<div style="font-size:32px;text-align:center;filter:drop-shadow(2px 2px 4px rgba(0,0,0,0.5));">' + emoji + '</div>',
                            className: 'pet-marker',
                            iconSize: [44, 44],
                            iconAnchor: [22, 22]
                        });
                        const marker = L.marker([lat, lng], {icon: icon}).addTo(map);
                        pets.set(id, {marker, color});
                        marker.on('click', function() { AndroidPetAR.abrirEncontro(roundId, id); });
                    }

                    function removePet(id, captured) {
                        const pet = pets.get(id);
                        if (!pet) return;
                        map.removeLayer(pet.marker);
                        pets.delete(id);

                        if (captured) {
                            const circle = L.circle(pet.marker.getLatLng(), {
                                radius: 20,
                                color: pet.color,
                                fillColor: pet.color,
                                fillOpacity: 0.6
                            }).addTo(map);
                            setTimeout(function() { map.removeLayer(circle); }, 800);
                        } else {
                            map.stop();
                        }
                    }

                    $markersJs
                </script>
            </body>
            </html>
        """.trimIndent()

        mapWebView.loadDataWithBaseURL(
            "https://openstreetmap.org",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun atualizarTela(views: FragmentChangeobjectBinding, round: MapRound) {
        views.scoreValue.text = round.score.toString()
        val escapedPet = round.escapedPet
        val pet = escapedPet ?: round.encounter
        views.mapWebView.isInvisible = escapedPet != null
        views.scorePanel.isVisible = escapedPet == null
        views.mapWebView.importantForAccessibility = if (pet == null) {
            View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        } else {
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        views.encounterOverlay.isVisible = pet != null
        if (pet == null) return

        val name = getString(pet.type.nameRes)
        views.encounterAction.isEnabled = true
        views.petIcon.text = pet.type.emoji
        views.treatIcon.isVisible = escapedPet == null
        views.treatIcon.translationY = 0f
        views.treatIcon.alpha = 0.6f

        if (escapedPet != null) {
            handler.removeCallbacks(hideCaptureToast)
            views.captureToast.clearAnimation()
            views.captureToast.isVisible = false
            mediaPlayer?.release()
            mediaPlayer = null
            views.encounterTitle.setText(R.string.game_over_title)
            views.encounterMessage.text = getString(R.string.game_over_reason, name, MapRound.MAX_TREATS)
            views.encounterStatus.text = getString(R.string.game_over_score, round.score)
            views.encounterAction.setText(R.string.play_again)
        } else {
            views.encounterTitle.text = name
            views.encounterMessage.setText(
                if (round.treatsRemaining == MapRound.MAX_TREATS) R.string.encounter_prompt
                else R.string.encounter_missed
            )
            views.encounterStatus.text = resources.getQuantityString(
                R.plurals.treats_remaining, round.treatsRemaining, round.treatsRemaining
            )
            views.encounterAction.setText(R.string.throw_treat)
        }
    }

    private fun lancarPetisco(views: FragmentChangeobjectBinding, round: MapRound) {
        val pet = round.encounter ?: return
        val result = round.throwTreat(Random.nextDouble() < PET_APPROACH_CHANCE) ?: return
        views.encounterAction.isEnabled = false
        views.treatIcon.alpha = 1f
        views.treatIcon.animate()
            .translationY(views.petIcon.y + views.petIcon.height / 2f - views.treatIcon.y)
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                if (binding !== views || sharedViewModel.mapRound !== round) return@withEndAction
                if (result != ThrowResult.MISSED) {
                    val captured = result == ThrowResult.CAPTURED
                    views.mapWebView.evaluateJavascript(
                        "typeof removePet === 'function' && removePet(${pet.id}, $captured);"
                    ) { value ->
                        if (captured && value == "false" &&
                            binding === views && sharedViewModel.mapRound === round &&
                            round.escapedPet == null
                        ) {
                            carregarMapa(views.mapWebView, round)
                        }
                    }
                    if (captured) {
                        tocarSomCaptura()
                        exibirAnimacaoCaptura(views, pet.type)
                    }
                }
                atualizarTela(views, round)
            }
            .start()
    }

    private fun tocarSomCaptura() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.latido_captura)
            mediaPlayer?.setOnCompletionListener { mp ->
                if (mediaPlayer === mp) mediaPlayer = null
                mp.release()
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Exibe animação visual de captura na tela.
     */
    private fun exibirAnimacaoCaptura(views: FragmentChangeobjectBinding, pet: PetType) {
        val captureToast = views.captureToast
        captureToast.text = getString(
            R.string.pet_captured, getString(pet.nameRes), MapRound.POINTS_PER_CAPTURE
        )
        captureToast.isVisible = true

        val animSet = AnimationSet(true)

        val scaleIn = ScaleAnimation(
            0.5f, 1.0f, 0.5f, 1.0f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply { duration = 300 }

        val fadeOut = AlphaAnimation(1.0f, 0.0f).apply {
            startOffset = 1200
            duration = 500
        }

        animSet.addAnimation(scaleIn)
        animSet.addAnimation(fadeOut)

        captureToast.startAnimation(animSet)

        handler.removeCallbacks(hideCaptureToast)
        handler.postDelayed(hideCaptureToast, 1700)
    }

    /**
     * Abre o encontro apenas para marcadores da partida atual.
     */
    inner class MapJsInterface(private val webView: WebView) {
        @JavascriptInterface
        fun abrirEncontro(roundId: String, petId: Int) {
            handler.post {
                val views = binding ?: return@post
                val round = sharedViewModel.mapRound ?: return@post
                if (views.mapWebView !== webView || round.id != roundId || views.encounterOverlay.isVisible) {
                    return@post
                }
                if (round.openEncounter(petId)) atualizarTela(views, round)
            }
        }
    }

    override fun onDestroyView() {
        val views = binding
        binding = null
        handler.removeCallbacks(hideCaptureToast)
        views?.treatIcon?.animate()?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        views?.mapWebView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        super.onDestroyView()
    }

    private companion object {
        const val PET_APPROACH_CHANCE = 0.4
    }
}