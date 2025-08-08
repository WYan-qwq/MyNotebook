package com.example.mynotebook.plan

import com.google.gson.annotations.SerializedName

data class Plan(
    @SerializedName("id") val id: Int,
    // 如果后端返回 user 对象，你可以用一个嵌套类或直接忽略它
    // @SerializedName("user") val user: UserInfo? = null,
    @SerializedName("createTime") val createTime: String,
    @SerializedName("date")      val date: String,
    @SerializedName("hour")      val hour: Int,
    @SerializedName("minute")    val minute: Int,
    @SerializedName("title")     val title: String,
    @SerializedName("details")   val details: String,
    @SerializedName("alarm")     val alarm: Int,
    @SerializedName("finished")  val finished: Int
)
