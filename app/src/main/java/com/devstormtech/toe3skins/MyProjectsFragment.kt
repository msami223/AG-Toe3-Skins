package com.devstormtech.toe3skins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class MyProjectsFragment : Fragment() {

    private lateinit var projectManager: ProjectManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var adapter: ProjectsAdapter

    // Callback to MainActivity to switch tabs/fragments
    var onProjectSelected: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_projects, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        projectManager = ProjectManager(requireContext())
        
        recyclerView = view.findViewById(R.id.recyclerProjects)
        emptyState = view.findViewById(R.id.emptyState)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        
        loadProjects()
    }
    
    // Refresh list when fragment becomes visible (onResume for first load/back stack)
    override fun onResume() {
        super.onResume()
        loadProjects()
    }
    
    // Refresh list when shown via hide/show transaction
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            loadProjects()
        }
    }

    private fun loadProjects() {
        val projects = projectManager.getAllProjects()
        
        // Show/hide empty state
        if (projects.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
        
        adapter = ProjectsAdapter(projects) { project ->
            onProjectSelected?.invoke(project.id)
        }
        recyclerView.adapter = adapter
    }

    inner class ProjectsAdapter(
        private val projects: List<ProjectMetadata>,
        private val onClick: (ProjectMetadata) -> Unit
    ) : RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder>() {

        inner class ProjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
            val txtName: TextView = view.findViewById(R.id.tvProjectName)
            val txtModel: TextView = view.findViewById(R.id.tvModelName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_project, parent, false)
            return ProjectViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
            val project = projects[position]
            
            holder.txtName.text = project.name
            holder.txtModel.text = project.truckDisplayName
            
            Glide.with(holder.itemView.context)
                .load(project.thumbnailPath)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true) // Always reload thumbnail as it might change
                .into(holder.imgThumbnail)
            
            holder.itemView.setOnClickListener { onClick(project) }
            
            // Long click to delete? (Maybe later)
            holder.itemView.setOnLongClickListener {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Project?")
                    .setMessage("Are you sure you want to delete '${project.name}'?")
                    .setPositiveButton("Delete") { _, _ ->
                         projectManager.deleteProject(project.id)
                         loadProjects() // Refresh
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
        }

        override fun getItemCount() = projects.size
    }
}
