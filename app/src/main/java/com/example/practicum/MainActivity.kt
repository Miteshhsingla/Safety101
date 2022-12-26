package com.example.practicum


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicum.contacts.AppDatabase
import com.example.practicum.contacts.Contact
import com.example.practicum.contacts.ContactAdapter
import com.example.practicum.databinding.ActivityMainBinding
import com.example.practicum.shakeServices.ReactivateService
import com.example.practicum.shakeServices.SensorService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), ContactAdapter.OnDeleteListener {

    private lateinit var binding: ActivityMainBinding
    private val PICK_CONTACT = 1

    private lateinit var db : AppDatabase
    private var contactsList = mutableListOf<Contact>()
    private lateinit var adapter : ContactAdapter

    @SuppressLint("Range")
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

        if (it.resultCode == Activity.RESULT_OK) {

            val contactData = it.data!!.data
            val cursor = managedQuery(contactData, null, null, null, null)
            if (cursor.moveToFirst()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val hasPhone =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))

                try {
                    var phone = ""

                    if (hasPhone.equals("1", ignoreCase = true)) {
                        val phones = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                            null,
                            null
                        )
                        phones!!.moveToFirst()
                        phone = phones.getString(
                            phones.getColumnIndex("data1")
                        )
                    }
                    val name =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

                    lifecycleScope.launch{
                        val c = Contact(name, phone)
                        db.contactDao().insertContact(c)
                        contactsList.add(c)
                        adapter.notifyDataSetChanged()
                    }

                } catch (ex: Exception) {
                    Log.d("TAG",ex.message.toString())
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_DENIED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_CONTACTS
                    ), 100
                )
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 100)
        }


        val sensorService = SensorService()
        val intent = Intent(this, sensorService.javaClass)
        if (!isMyServiceRunning(sensorService.javaClass)) {
            startService(intent)
        }

        db = AppDatabase.getInstance(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)


        lifecycleScope.launch {
            contactsList = db.contactDao().getAllContacts().toMutableList()
            adapter = ContactAdapter(this@MainActivity, contactsList, this@MainActivity)
            binding.recyclerView.adapter = adapter
        }
        binding.btn2.setOnClickListener{
            var intent = Intent(this,MainActivity2::class.java)
            startActivity(intent)
        }
        binding.addBtn.setOnClickListener {

            lifecycleScope.launch {
                if (db.contactDao().getCount() != 5) {
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                    launcher.launch(intent)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Can't Add more than 5 Contacts",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }
    }


    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                Log.i("Service status", "Running")
                return true
            }
        }
        Log.i("Service status", "Not running")
        return false
    }

    override fun onDestroy() {
        val broadcastIntent = Intent()
        broadcastIntent.action = "restartservice"
        broadcastIntent.setClass(this, ReactivateService::class.java)
        this.sendBroadcast(broadcastIntent)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permissions Denied!\n Can't use the App!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


    override fun onDelete(c: Contact) {
        lifecycleScope.launch {
            db.contactDao().deleteContact(c)
        }
    }
}