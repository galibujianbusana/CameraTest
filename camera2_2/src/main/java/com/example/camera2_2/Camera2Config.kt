package com.example.camera2_2

/**
 * Created by cxk on 2017/12/8.
 * 自定义的配置文件
 */
object Camera2Config {
    var RECORD_MAX_TIME = 1000 //录制的总时长秒数，单位秒，默认10秒
    var RECORD_MIN_TIME = 2 //最小录制时长，单位秒，默认2秒
    var RECORD_PROGRESS_VIEW_COLOR = R.color.colorAccent //进度条颜色，默认蓝色
    var PREVIEW_MAX_HEIGHT = 1000 //最大高度预览尺寸，默认大于1000的第一个
    var PATH_SAVE_VIDEO =
        Camera2Util.camera2Path //小视频存放地址，不设置的话默认在根目录的Camera2文件夹
    var PATH_SAVE_PIC =
        Camera2Util.camera2Path //图片保存地址，不设置的话默认在根目录的Camera2文件夹
    var ACTIVITY_AFTER_CAPTURE //拍照完成后需要跳转的Activity,一般这个activity做处理照片或者视频用
            : Class<*>? = null
    var INTENT_PATH_SAVE_VIDEO = "INTENT_PATH_SAVE_VIDEO" //Intent跳转可用
    var INTENT_PATH_SAVE_PIC = "INTENT_PATH_SAVE_PIC" //Intent跳转可用
    var ENABLE_RECORD = true //是否需要录像功能
    var ENABLE_CAPTURE = true //是否需要拍照功能
}