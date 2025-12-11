package adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.devstormtech.toe3skins.DownloadItem
import com.devstormtech.toe3skins.R

class DownloadsAdapter(
    private val downloads: List<DownloadItem>,
    private val onClick: (DownloadItem) -> Unit
) : RecyclerView.Adapter<DownloadsAdapter.DownloadViewHolder>() {

    inner class DownloadViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvDownloadIcon)
        val tvFilename: TextView = view.findViewById(R.id.tvDownloadFilename)
        val tvTimestamp: TextView = view.findViewById(R.id.tvDownloadTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download, parent, false)
        return DownloadViewHolder(view)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        val download = downloads[position]
        
        // Set icon based on source
        holder.tvIcon.text = if (download.source == "Editor") "✨" else "📥"
        
        // Set filename
        holder.tvFilename.text = download.filename
        
        // Set relative timestamp
        val relativeTime = DateUtils.getRelativeTimeSpanString(
            download.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        holder.tvTimestamp.text = relativeTime
        
        // Set click listener
        holder.itemView.setOnClickListener { onClick(download) }
    }

    override fun getItemCount() = downloads.size
}
