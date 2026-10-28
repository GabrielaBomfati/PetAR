package com.ifpr.androidapptemplatear.ui

import androidx.fragment.app.Fragment
import com.ifpr.androidapptemplatear.R
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.ar.core.Config
import com.ifpr.androidapptemplatear.databinding.FragmentCameraBinding
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import java.io.File

class CameraFragment : Fragment() {

    private lateinit var sceneView: ArSceneView
    // modelNode é nullable para evitar UninitializedPropertyAccessException
    private var modelNode: ArModelNode? = null
    private lateinit var placeButton: ExtendedFloatingActionButton
    private lateinit var btnChangeModel: FloatingActionButton

    private lateinit var sharedViewModel: SharedModelViewModel

    private val CAMERA_PERMISSION_CODE = 1001

    private var currentModelFile: String = "neutrofilo.glb"

    private var _binding: FragmentCameraBinding? = null

    // Som e pontuação para captura de pets na RA
    private var mediaPlayer: MediaPlayer? = null
    private var capturasPetsRA = 0

    // Impede carregamentos simultâneos que causam crash no Filament
    private var isLoadingModel = false

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // CORREÇÃO: super.onCreate não deve ser chamado aqui, usa super.onCreateView
        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        sceneView = view.findViewById<ArSceneView?>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.DISABLED
        }

        placeButton = view.findViewById(R.id.place)
        btnChangeModel = view.findViewById(R.id.btnChangeModel)

        placeButton.setOnClickListener {
            placeModel()
        }

        btnChangeModel.setOnClickListener {
            mostrarDialogoEscolherPet()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // CORREÇÃO: Observer registrado aqui (após onCreateView), usando viewLifecycleOwner
        // que só existe a partir deste ponto. sceneView também já está pronto.
        sharedViewModel = ViewModelProvider(requireActivity())[SharedModelViewModel::class.java]
        sharedViewModel.selectedModel.observe(viewLifecycleOwner) { path ->
            if (!path.isNullOrBlank()) {
                currentModelFile = path
                loadModel(path)
            }
        }

        checkCameraPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mediaPlayer?.release()
        mediaPlayer = null
        // Limpa o nó do modelo ao destruir a view
        try {
            modelNode?.let { sceneView.removeChild(it) }
            modelNode?.destroy()
            modelNode = null
        } catch (e: Exception) {
            Log.e("CameraFragment", "Erro ao destruir modelNode: ${e.message}")
        }
    }

    private fun placeModel() {
        // CORREÇÃO: Verifica se modelNode foi inicializado antes de usar
        val node = modelNode ?: run {
            Toast.makeText(requireContext(), "Modelo ainda carregando...", Toast.LENGTH_SHORT).show()
            return
        }
        node.anchor()
        node.isEditable = true
        node.isScaleEditable = false
        sceneView.planeRenderer.isVisible = false
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            initAR()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initAR()
        } else {
            if (isAdded) {
                Toast.makeText(requireContext(), "Permissão de câmera negada", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initAR() {
        loadModel(currentModelFile)
    }

    /**
     * Exibe diálogo para o usuário escolher qual pet (modelo 3D)
     * mostrar na câmera AR.
     */
    private fun mostrarDialogoEscolherPet() {
        val nomes = arrayOf("🐱 Gato", "🐕 Golden", "🐶 Cachorro", "🔬 Neutrófilo")
        val arquivos = arrayOf("gato_g.glb", "golden.glb", "cachorro_g.glb", "neutrofilo.glb")

        AlertDialog.Builder(requireContext())
            .setTitle("Escolher Pet")
            .setItems(nomes) { _, which ->
                val arquivo = arquivos[which]
                currentModelFile = arquivo
                // Atualiza via ViewModel (o observer faz o loadModel)
                sharedViewModel.selectModel(arquivo)
            }
            .show()
    }

    /**
     * Toca o som de captura (latido) quando o pet é tocado na RA.
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
            Log.e("CameraFragment", "Erro ao tocar som: ${e.message}")
        }
    }

    /**
     * Retorna o nome amigável do pet com base no nome
     * do arquivo do modelo 3D.
     */
    private fun nomeDoPet(fileName: String): String {
        return when {
            fileName.contains("gato", ignoreCase = true) -> "🐱 Gato"
            fileName.contains("golden", ignoreCase = true) -> "🐕 Golden"
            fileName.contains("cachorro", ignoreCase = true) -> "🐶 Cachorro"
            else -> "🐾 Pet"
        }
    }

    private fun loadModel(fileName: String) {
        // Ignora se já está carregando — evita crash por carregamento simultâneo no Filament
        if (isLoadingModel) {
            Log.w("CameraFragment", "Já está carregando um modelo, aguarde.")
            return
        }
        isLoadingModel = true

        // Remove o nó antigo SEM chamar destroy() — destroy() durante carregamento
        // assíncrono de textura causa crash nativo no libgltfio-jni.so (Filament)
        try {
            modelNode?.let { sceneView.removeChild(it) }
        } catch (e: Exception) {
            Log.w("CameraFragment", "Aviso ao remover modelNode anterior: ${e.message}")
        }
        modelNode = null

        // CORREÇÃO: Valida o caminho do arquivo antes de tentar carregar
        val filePath = getFilePath(requireContext(), fileName)
        if (filePath == null) {
            Log.e("CameraFragment", "Arquivo não encontrado: $fileName")
            isLoadingModel = false
            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    "Erro: modelo '$fileName' não encontrado.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        Log.d("CameraFragment", "Carregando modelo: $filePath")

        val newNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = filePath,
                centerOrigin = Position(0f, 0f, -1f), // CORREÇÃO: posição à frente do usuário
            ) {
                // Callback na thread principal após carregamento completo
                isLoadingModel = false
                if (isAdded) {
                    sceneView.planeRenderer.isVisible = true
                    Log.d("CameraFragment", "Modelo carregado com sucesso: $fileName")
                }
            }

            isEditable = true
            isScaleEditable = false

            // Detectar toque no modelo 3D para capturar o pet
            onTap = { _, _ ->
                if (isAdded) {
                    capturasPetsRA++
                    val nome = nomeDoPet(fileName)
                    tocarSomCaptura()
                    Toast.makeText(
                        requireContext(),
                        "$nome capturado! 🎉 Total: $capturasPetsRA",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        modelNode = newNode
        sceneView.addChild(newNode)
    }

    /**
     * Retorna o caminho do arquivo GLB para uso no ARSceneView.
     * Busca primeiro nos assets, depois no filesDir interno.
     * Retorna null se o arquivo não for encontrado em nenhum lugar.
     */
    private fun getFilePath(context: Context?, fileName: String): String? {
        if (context == null) return null
        return try {
            // Tenta abrir nos assets para verificar se existe
            context.assets.open(fileName).close()
            // Retorna apenas o nome; ARSceneView busca nos assets automaticamente
            fileName
        } catch (e: Exception) {
            // Não está nos assets — procura no filesDir interno
            val internalFile = File(context.filesDir, fileName)
            if (internalFile.exists()) {
                Uri.fromFile(internalFile).toString()
            } else {
                // Arquivo não encontrado em lugar nenhum
                null
            }
        }
    }
}
