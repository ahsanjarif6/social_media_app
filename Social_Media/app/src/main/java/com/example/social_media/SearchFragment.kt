package com.example.social_media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment(R.layout.fragment_search) {
    private lateinit var editTextSearch: EditText
    private lateinit var buttonSendSearch: ImageButton
    private lateinit var recyclerViewProfiles: RecyclerView

    private lateinit var adapter: SearchAdapter

    private val users = mutableListOf<User>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextSearch = view.findViewById(R.id.editTextSearch)
        buttonSendSearch = view.findViewById(R.id.buttonSendSearch)
        recyclerViewProfiles = view.findViewById(R.id.recyclerViewProfiles)

        adapter = SearchAdapter(users) { selectedUser ->
            NavigationUtils.handleUserClick(requireContext(), selectedUser.id)
        }

        recyclerViewProfiles.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewProfiles.adapter = adapter

        buttonSendSearch.setOnClickListener {
            val query = editTextSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchUsers(query)
            }
        }
    }

    private fun searchUsers(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = SupabaseClient.client
                    .from("profiles")
                    .select {
                        filter {
                            ilike("email", "%$query%")
                        }
                    }
                    .decodeList<User>()

                withContext(Dispatchers.Main) {
                    users.clear()
                    users.addAll(result)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
