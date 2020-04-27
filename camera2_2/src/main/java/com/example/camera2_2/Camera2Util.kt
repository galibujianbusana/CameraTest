package com.example.camera2_2

import android.os.Environment
import android.util.Size
import java.io.File
import java.util.*

/**
 * Created by cxk on 2017/12/8.
 *
 *
 * 这里为了方便，将部分方法封装到这个Util里面
 */
class Camera2Util {
    //选择合适的视频size，并且不能大于1080p
    private fun chooseVideoSize(choices: Array<Size>): Size {
        for (size in choices) {
            if (size.width == size.height * 4 / 3 && size.width <= 1080) {
                return size
            }
        }
        return choices[choices.size - 1]
    }

    //选择sizeMap中大于并且最接近width和height的size
    private fun getOptimalSize(
        sizeMap: Array<Size>,
        width: Int,
        height: Int
    ): Size {
        val sizeList: MutableList<Size> =
            ArrayList()
        for (option in sizeMap) {
            if (width > height) {
                if (option.width > width && option.height > height) {
                    sizeList.add(option)
                }
            } else {
                if (option.width > height && option.height > width) {
                    sizeList.add(option)
                }
            }
        }
        return if (sizeList.size > 0) {
            Collections.min(
                sizeList
            ) { lhs, rhs ->
                java.lang.Long.signum(
                    lhs.width * lhs.height - rhs.width * rhs.height
                        .toLong()
                )
            }
        } else sizeMap[0]
    }

    companion object {
        // 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择，activity我们已经固定了方向，所以这里无需在做判断
        protected fun getCloselyPreSize(
            sizeMap: Array<Size>,
            surfaceWidth: Int,
            surfaceHeight: Int
        ): Size? {
            val ReqTmpWidth: Int
            val ReqTmpHeight: Int
            ReqTmpWidth = surfaceHeight
            ReqTmpHeight = surfaceWidth
            //先查找preview中是否存在与surfaceview相同宽高的尺寸
            for (size in sizeMap) {
                if (size.width == ReqTmpWidth && size.height == ReqTmpHeight) {
                    return size
                }
            }

            // 得到与传入的宽高比最接近的size
            val reqRatio = ReqTmpWidth.toFloat() / ReqTmpHeight
            var curRatio: Float
            var deltaRatio: Float
            var deltaRatioMin = Float.MAX_VALUE
            var retSize: Size? = null
            for (size in sizeMap) {
                curRatio = size.width.toFloat() / size.height
                deltaRatio = Math.abs(reqRatio - curRatio)
                if (deltaRatio < deltaRatioMin) {
                    deltaRatioMin = deltaRatio
                    retSize = size
                }
            }
            return retSize
        }

        /**
         * 核心方法，这里是通过从sizeMap中获取和Textureview宽高比例相同的map，然后在获取接近自己想获取到的尺寸
         * 之所以这么做是因为我们要确保预览尺寸不要太大，这样才不会太卡
         *
         * @param sizeMap
         * @param surfaceWidth
         * @param surfaceHeight
         * @param maxHeight
         * @return
         */
        fun getMinPreSize(
            sizeMap: Array<Size>,
            surfaceWidth: Int,
            surfaceHeight: Int,
            maxHeight: Int
        ): Size? {
            // 得到与传入的宽高比最接近的size
            val reqRatio = surfaceWidth.toFloat() / surfaceHeight
            var curRatio: Float
            val sizeList: MutableList<Size> =
                ArrayList()
            var retSize: Size? = null
            for (size in sizeMap) {
                curRatio = size.height.toFloat() / size.width
                if (reqRatio == curRatio) {
                    sizeList.add(size)
                }
            }
            if (sizeList != null && sizeList.size != 0) {
                for (i in sizeList.indices.reversed()) {
                    //取Size宽度大于1000的第一个数,这里我们获取大于maxHeight的第一个数，理论上我们是想获取size.getWidth宽度为1080或者1280那些，因为这样的预览尺寸已经足够了
                    if (sizeList[i].width >= maxHeight) {
                        retSize = sizeList[i]
                        break
                    }
                }

                //可能没有宽度大于maxHeight的size,则取相同比例中最小的那个size
                if (retSize == null) {
                    retSize = sizeList[sizeList.size - 1]
                }
            } else {
                retSize = getCloselyPreSize(
                    sizeMap,
                    surfaceWidth,
                    surfaceHeight
                )
            }
            return retSize
        }

        /**
         * 使用Camera2录制和所拍的照片都会在这里
         */
        val camera2Path: String
            get() {
                val picturePath = Environment.getExternalStorageDirectory()
                    .absolutePath + "/CameraV2/"
                val file = File(picturePath)
                if (!file.exists()) {
                    file.mkdirs()
                }
                return picturePath
            }

        /**
         * 判断传入的地址是否已经有这个文件夹，没有的话需要创建
         */
        fun createSavePath(path: String?) {
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
        }
    }
}