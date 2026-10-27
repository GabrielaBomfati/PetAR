package com.ifpr.androidapptemplatear.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ifpr.androidapptemplatear.R

class ChangeObjectFragment : Fragment() {

    private lateinit var mapWebView: WebView
    private lateinit var scoreValue: TextView
    private lateinit var scorePanel: LinearLayout
    private lateinit var captureToast: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var mediaPlayer: MediaPlayer? = null
    private var score = 0

    private val LOCATION_PERMISSION_CODE = 2001

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

        mapWebView = view.findViewById(R.id.mapWebView)
        scoreValue = view.findViewById(R.id.scoreValue)
        scorePanel = view.findViewById(R.id.scorePanel)
        captureToast = view.findViewById(R.id.captureToast)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Aplicar backgrounds arredondados programaticamente
        aplicarBackgroundArredondado(scorePanel, Color.parseColor("#CC1B1B2F"))
        aplicarBackgroundArredondado(captureToast, Color.parseColor("#CC2E7D32"))

        configurarWebView()
        verificarPermissaoLocalizacao()
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
    private fun configurarWebView() {
        mapWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        mapWebView.webViewClient = WebViewClient()
        mapWebView.webChromeClient = WebChromeClient()

        // Interface JavaScript para comunicar capturas do mapa para o app
        mapWebView.addJavascriptInterface(
            MapJsInterface(),
            "AndroidPetAR"
        )
    }

    private fun verificarPermissaoLocalizacao() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_CODE
            )
        } else {
            obterLocalizacaoECarregarMapa()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            obterLocalizacaoECarregarMapa()
        } else {
            // Sem permissão: usa localização padrão (São Paulo)
            carregarMapa(-23.5505, -46.6333)
        }
    }

    @SuppressLint("MissingPermission")
    private fun obterLocalizacaoECarregarMapa() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                carregarMapa(location.latitude, location.longitude)
            } else {
                // Fallback: localização padrão
                carregarMapa(-23.5505, -46.6333)
            }
        }.addOnFailureListener {
            carregarMapa(-23.5505, -46.6333)
        }
    }

    /**
     * Carrega o mapa OpenStreetMap via Leaflet.js com marcadores
     * de pets aleatórios ao redor da posição do usuário.
     */
    private fun carregarMapa(lat: Double, lng: Double) {
        val petModels = listOf(
            PetInfo("🐱", "Gato", "gato_g.glb", "#FF6B9D"),
            PetInfo("🐕", "Golden", "golden.glb", "#FFB347"),
            PetInfo("🐶", "Cachorro", "cachorro_g.glb", "#87CEEB")
        )

        // Gera posições aleatórias ao redor do usuário
        val markersJs = StringBuilder()
        val random = java.util.Random()

        for (i in 0 until 15) {
            val pet = petModels[i % petModels.size]
            val offsetLat = (random.nextDouble() - 0.5) * 0.008
            val offsetLng = (random.nextDouble() - 0.5) * 0.008
            val petLat = lat + offsetLat
            val petLng = lng + offsetLng

            markersJs.append("""
                (function() {
                    var petIcon = L.divIcon({
                        html: '<div style="font-size:32px;text-align:center;filter:drop-shadow(2px 2px 4px rgba(0,0,0,0.5));cursor:pointer;">${pet.emoji}</div>',
                        className: 'pet-marker',
                        iconSize: [44, 44],
                        iconAnchor: [22, 22]
                    });
                    var m = L.marker([$petLat, $petLng], {icon: petIcon}).addTo(map);
                    m.bindPopup('<div style="text-align:center;font-size:14px;"><b>${pet.emoji} ${pet.nome}</b><br><span style="color:#666;">Toque para capturar!</span></div>');
                    m.on('click', function() {
                        AndroidPetAR.capturarPet('${pet.nome}', '${pet.arquivo}');
                        map.removeLayer(m);
                        
                        // Efeito visual de captura no mapa
                        var captureCircle = L.circle([$petLat, $petLng], {
                            radius: 20,
                            color: '${pet.cor}',
                            fillColor: '${pet.cor}',
                            fillOpacity: 0.6
                        }).addTo(map);
                        setTimeout(function() { map.removeLayer(captureCircle); }, 800);
                    });
                })();
            """)
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

    /**
     * Toca o som de captura do pet.
     */
    private fun tocarSomCaptura() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.latido_captura)
            mediaPlayer?.setOnCompletionListener { mp ->
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
    private fun exibirAnimacaoCaptura(nomePet: String) {
        captureToast.text = "🎉 $nomePet capturado!"
        captureToast.visibility = View.VISIBLE

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

        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                captureToast.visibility = View.GONE
            }
        }, 1700)
    }

    /**
     * Interface JavaScript chamada pelo mapa quando
     * um pet é tocado/clicado.
     */
    inner class MapJsInterface {

        @JavascriptInterface
        fun capturarPet(nome: String, arquivo: String) {
            Handler(Looper.getMainLooper()).post {
                if (!isAdded) return@post

                score++
                scoreValue.text = score.toString()

                tocarSomCaptura()
                exibirAnimacaoCaptura(nome)

                Toast.makeText(
                    requireContext(),
                    "🐾 $nome capturado! Pontos: $score",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Dados de cada tipo de pet no mapa.
     */
    data class PetInfo(
        val emoji: String,
        val nome: String,
        val arquivo: String,
        val cor: String
    )

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        mapWebView.destroy()
    }
}