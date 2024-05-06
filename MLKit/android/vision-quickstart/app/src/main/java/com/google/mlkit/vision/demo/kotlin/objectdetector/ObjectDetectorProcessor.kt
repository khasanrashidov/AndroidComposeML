/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.kotlin.objectdetector

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.demo.GraphicOverlay
import com.google.mlkit.vision.demo.kotlin.VisionProcessorBase
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase
import java.io.IOException

/**
 * # A processor to run object detector.
 *
 * 1. **InputImage Creation**: An `InputImage` is created from a frame captured from the device's camera.
 *  This image is then passed to the `detectInImage` function.
 * 2. **Image Processing**: The `detectInImage` function processes the `InputImage` using
 *  the `ObjectDetector` instance (`detector`). This is done by calling `detector.process(image)`,
 *  which processes the image and returns a `Task` object. The `Task` object represents a pending
 *  operation that produces a list of `DetectedObject` when it's complete.
 * 3. **Task Completion**: Once the `Task` is complete, it triggers the `addOnSuccessListener` or
 * `addOnFailureListener` methods. If the task is successful, it triggers the `addOnSuccessListener`
 *  method, passing the list of `DetectedObject` to it.
 * 4. **Success Handling**: In the `addOnSuccessListener` method, the `onSuccess` function of
 *  the `ObjectDetectorProcessor` class is called. This function receives the list of `DetectedObject`
 *  and a `GraphicOverlay`. It then adds an `ObjectGraphic` for each `DetectedObject` to the `GraphicOverlay`.
 * 5. **Failure Handling**: If the `Task` fails, it triggers the `addOnFailureListener` method,
 *  which calls the `onFailure` function of the `ObjectDetectorProcessor` class.
 * 6. **UI Update**: After the `GraphicOverlay` is updated with the new graphics, it's invalidated
 *  with `postInvalidate()`, which causes it to be redrawn on the screen.
 */



/** A processor to run object detector.  */
class ObjectDetectorProcessor(context: Context, options: ObjectDetectorOptionsBase) :
  VisionProcessorBase<List<DetectedObject>>(context) {

  private val detector: ObjectDetector = ObjectDetection.getClient(options)

  override fun stop() {
    super.stop()
    try {
      detector.close()
    } catch (e: IOException) {
      Log.e(
        TAG,
        "Exception thrown while trying to close object detector!",
        e
      )
    }
  }

  override fun detectInImage(image: InputImage): Task<List<DetectedObject>> {
    return detector.process(image)
  }

  override fun onSuccess(results: List<DetectedObject>, graphicOverlay: GraphicOverlay) {
    for (result in results) {
      graphicOverlay.add(ObjectGraphic(graphicOverlay, result))
    }
  }

  override fun onFailure(e: Exception) {
    Log.e(TAG, "Object detection failed!", e)
  }

  companion object {
    private const val TAG = "ObjectDetectorProcessor"
  }
}
