package com.devstormtech.toe3skins

data class TruckModel(
    val id: String,
    val displayName: String,
    val templateResource: Int
) {
    companion object {
        fun getAllTrucks(): List<TruckModel> = listOf(
            TruckModel("stream_st", "STREAM/ST", R.drawable.template_stream_st),
            TruckModel("dawn_df", "DAWN/DF", R.drawable.template_dawn_df),
            TruckModel("moon_tha", "MOON/THA", R.drawable.template_moon_tha),
            TruckModel("moon_thx", "MOON/THX", R.drawable.template_moon_thx),
            TruckModel("fiora_fiman", "FIORA FI-MAN", R.drawable.template_fiora_fiman),
            TruckModel("volcano_vn", "VOLCANO/VN", R.drawable.template_volcano_vn),
            TruckModel("stream_rt", "STREAM/RT", R.drawable.template_stream_rt),
            TruckModel("stream_rt_legend_1995", "STREAM/RT LEGEND 1995", R.drawable.template_stream_rt_legend_1995),
            TruckModel("stream_rt_legend_2004", "STREAM/RT LEGEND 2004", R.drawable.template_stream_rt_legend_2004),
            TruckModel("stream_rt_legend_2013", "STREAM/RT LEGEND 2013", R.drawable.template_stream_rt_legend_2013),
            TruckModel("renovate_ranger_2013", "RENOVATE-R-RANGER 2013", R.drawable.template_renovate_ranger_2013),
            TruckModel("renovate_ranger_2021", "RENOVATE-R-RANGER 2021", R.drawable.template_renovate_ranger_2021),
            TruckModel("merieles_arox", "MERIELES/AROX", R.drawable.template_merieles_arox),
            TruckModel("merieles_antares", "MERIELES/ANTARES", R.drawable.template_merieles_antares)
        )
    }
}
