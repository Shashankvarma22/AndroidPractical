package com.example.fragmentsdemo

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fragmentsdemo.databinding.FragmentSystemInfoBinding

class SystemInfoFragment : Fragment() {

    private var _binding: FragmentSystemInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSystemInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateBatteryInfo()
    }

    private fun updateBatteryInfo() {
        val batteryStatus = requireContext().registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryStatus?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percentage = level * 100 / scale
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val health = it.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val voltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val temperature = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
            val technology = it.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            val present = it.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)

            val info = """
                Battery Level : $percentage%
                Status : ${batteryStatusToString(status)}
                Health : ${batteryHealthToString(health)}
                Plugged : ${pluggedTypeToString(plugged)}
                Voltage : $voltage mV
                Temperature : $temperature °C
                Technology : $technology
                Battery Present : $present
            """.trimIndent()

            binding.tvBatteryInfo.text = info
        }
    }

    private fun batteryStatusToString(status: Int): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
        else -> "Unknown"
    }

    private fun batteryHealthToString(health: Int): String = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        else -> "Unknown"
    }

    private fun pluggedTypeToString(type: Int): String = when (type) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> "Not Plugged"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
