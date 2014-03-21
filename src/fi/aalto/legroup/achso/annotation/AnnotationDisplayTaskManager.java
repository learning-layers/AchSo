/**
 * Copyright 2013 Aalto university, see AUTHORS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.annotation;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AnnotationDisplayTaskManager {

    private static final int POOLS = 5;
    private static final int FADE_TICK_COUNT = 10;
    private static AnnotationDisplayTaskManager mInstance;
    private static ScheduledThreadPoolExecutor mExecutor;
    private static List<Future> mFutures;
    private Handler mHandler;

    private AnnotationDisplayTaskManager() {
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                AnnotationTask task = (AnnotationTask) msg.obj;
                if (task != null) {
                    synchronized (task.annotation) { // Annotation may be killed in the middle of actions if not synchronized
                        if (task.annotation.isAlive()) {
                            switch (task.type) {
                                case AnnotationStart:
                                    task.startAnnotation();
                                    break;
                                case AnnotationFadeTick:
                                    task.fadeTick();
                                    break;
                                case AnnotationEnd:
                                    task.endAnnotation();
                                    break;
                            }
                        }
                    }
                }
            }
        };
    }

    static {
        /*
        mInstance = new AnnotationDisplayTaskManager();
        mExecutor = new ScheduledThreadPoolExecutor(POOLS, Executors.defaultThreadFactory());
        mFutures = new ArrayList<Future>();
        */
    }

    public static AnnotationDisplayTaskManager getInstance() {
        return mInstance;
    }

    private Future addDelayedAnnotation(TaskType t, Annotation a, AnnotationSurfaceHandler h, long delayMs) {
        return mExecutor.schedule(new AnnotationTask(t, a, h), delayMs, TimeUnit.MILLISECONDS);
    }

    public void addDelayedAnnotationStart(Annotation a, AnnotationSurfaceHandler h, long delayMs) {
        mFutures.add(addDelayedAnnotation(TaskType.AnnotationStart, a, h, delayMs));
    }

    public void addDelayedAnnotationEnd(Annotation a, AnnotationSurfaceHandler h, long delayMs) {
        mFutures.add(addDelayedAnnotation(TaskType.AnnotationFadeTick, a, h, delayMs));
    }

    public void cancelAll() {
        for (Future f : mFutures) {
            f.cancel(true);
        }
        mFutures = new ArrayList<Future>();
        mExecutor.purge();
    }

    public enum TaskType {
        AnnotationStart,
        AnnotationFadeTick,
        AnnotationEnd,
    }

    public class AnnotationTask implements Runnable {
        public final Annotation annotation;
        public AnnotationSurfaceHandler annotationSurfaceHandler;
        public TaskType type;

        AnnotationTask(TaskType t, Annotation a, AnnotationSurfaceHandler h) {
            annotation = a;
            annotationSurfaceHandler = h;
            type = t;
        }

        public void run() {
            Message m = new Message();
            m.obj = this;
            synchronized (annotation) { // Annotation may be killed in the middle of actions if not synchronized
                if (annotation.isAlive()) {
                    AnnotationDisplayTaskManager.getInstance().mHandler.sendMessage(m);
                }
            }
        }

        private void startAnnotation() {
            annotation.setVisible(true);
            annotationSurfaceHandler.draw();
            AnnotationDisplayTaskManager.getInstance().addDelayedAnnotationEnd(annotation, annotationSurfaceHandler, annotation.getDuration());
        }


        private void fadeTick() {
            annotation.setOpacity(annotation.getOpacity() - (100 / FADE_TICK_COUNT));
            if (annotation.getOpacity() <= 0)
                mFutures.add(addDelayedAnnotation(TaskType.AnnotationEnd, annotation, annotationSurfaceHandler, 0));
            else {
                mFutures.add(addDelayedAnnotation(TaskType.AnnotationFadeTick, annotation, annotationSurfaceHandler, Annotation.ANNOTATION_FADE_DURATION_MILLISECONDS / FADE_TICK_COUNT));
                annotationSurfaceHandler.draw();
            }
        }

        private void endAnnotation() {
            annotation.setVisible(false);
            annotationSurfaceHandler.draw();
        }

    }
}
