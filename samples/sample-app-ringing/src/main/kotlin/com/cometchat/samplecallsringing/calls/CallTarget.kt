package com.cometchat.samplecallsringing.calls

/** Who a call rings: a user ([id] is the UID) or a group ([id] is the GUID). */
data class CallTarget(
    val id: String,
    val name: String,
    val avatar: String?,
    val isGroup: Boolean
)
