import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import kotlinx.coroutines.delay

class UploadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        // Simulate a long-running task
        showMessage()
        return Result.success()
    }

    private suspend fun showMessage() {
        delay(2000) // Simulate work
//        Log.d("TrackingApp", "I am working too")
    }
}