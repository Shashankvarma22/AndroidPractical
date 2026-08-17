package com.example.fragmentsdemo

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.fragmentsdemo.databinding.FragmentDashboardBinding
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.TimeUnit

@UnstableApi
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val airplaneModeReceiver = AirplaneModeReceiver()
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                playMusicViaController()
            } else {
                Toast.makeText(context, "Notification permission required for music", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBatteryInfo.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dashboard_to_systemInfoFragment)
        }

        binding.btnContacts.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, getString(R.string.contacts_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSpecificContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode("79818785557"))
            intent.setData(uri)
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, getString(R.string.contacts_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDial.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:79818785557"))
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, getString(R.string.dialer_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, getString(R.string.camera_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBrowser.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/"))
            startActivity(intent)
        }

        binding.btnStartService.setOnClickListener {
            val intent = Intent(requireContext(), DemoService::class.java)
            requireActivity().startService(intent)
            Toast.makeText(context, "Service Started", Toast.LENGTH_SHORT).show()
        }

        binding.btnStopService.setOnClickListener {
            val intent = Intent(requireContext(), DemoService::class.java)
            requireActivity().stopService(intent)
            Toast.makeText(context, "Service Stopped", Toast.LENGTH_SHORT).show()
        }

        binding.btnStartMusic.setOnClickListener {
            checkPermissionThenPlayMusic()
        }

        binding.btnStopMusic.setOnClickListener {
            controller?.pause()
            val intent = Intent(requireContext(), MediaPlayerService::class.java)
            requireActivity().stopService(intent)
            Snackbar.make(binding.root, "Music Stopped", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnStartBackgroundWorker.setOnClickListener {
            val inputData = workDataOf("LogWorkerMessage" to "This is a log message")
            val workRequest = PeriodicWorkRequestBuilder<LogWorker>(15, TimeUnit.MINUTES)
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "LogWorker",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                workRequest
            )
            Toast.makeText(context, "Log Worker Enqueued", Toast.LENGTH_SHORT).show()
        }

        binding.btnDownload.setOnClickListener {
            val downloadInputData = workDataOf(
                DownloadWorker.KEY_DOWNLOAD_URL to "https://drive.google.com/uc?export=download&id=10Ver7gLgwy9H6_bTNnO0b0ng7hCr3Tfx",
                DownloadWorker.KEY_FILE_NAME to "hanuman.jpg"
            )
            val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(downloadInputData)
                .build()
            val workManager = WorkManager.getInstance(requireContext())
            workManager.enqueue(downloadRequest)
            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
            workManager.getWorkInfoByIdLiveData(downloadRequest.id).observe(viewLifecycleOwner) { workInfo ->
                if (workInfo == null) return@observe
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val msg = workInfo.outputData.getString(DownloadWorker.KEY_RESULT_MESSAGE) ?: "Download Completed"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    WorkInfo.State.FAILED -> {
                        val msg = workInfo.outputData.getString(DownloadWorker.KEY_RESULT_MESSAGE) ?: "Download Failed"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }

        binding.btnFragmentDemo.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dashboard_to_fragmentDemoFragment)
        }

        binding.btnLogout.setOnClickListener {
            // Find NavController and pop all way back to Login
            findNavController().navigate(R.id.action_nav_dashboard_to_loginFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        ContextCompat.registerReceiver(requireContext(), airplaneModeReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        initializeController()
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(airplaneModeReceiver)
        releaseController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(requireContext(), ComponentName(requireContext(), MediaPlayerService::class.java))
        controllerFuture = MediaController.Builder(requireContext(), sessionToken).buildAsync()
    }

    private fun releaseController() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
    }

    private fun checkPermissionThenPlayMusic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                playMusicViaController()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            playMusicViaController()
        }
    }

    private fun playMusicViaController() {
        controller?.let {
            if (!it.isPlaying) {
                it.play()
            }
            Snackbar.make(binding.root, "Music Started", Snackbar.LENGTH_SHORT).show()
        } ?: run {
            controllerFuture?.addListener({
                controller?.play()
                Snackbar.make(binding.root, "Music Started", Snackbar.LENGTH_SHORT).show()
            }, MoreExecutors.directExecutor())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
