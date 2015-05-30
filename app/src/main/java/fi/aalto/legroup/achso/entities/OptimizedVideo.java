package fi.aalto.legroup.achso.entities;

public class OptimizedVideo {

    private private String manifestUri;
    private String videoUri;
    private String thumbUri;
    private UUID id;
    private String title;
    private String genre;
    private String tag;
    private long dateInMs;
    private VideoRepository repository;
    private User author;
    private Location location;
    private long[] annotationTime;
    private float[] annotationX;
    private float[] annotationY;
    private String[] annotationText;
    private OptimizedUser[] annotationAuthor;

    static ArrayList<User> userPool = new ArrayList<User>();

    static User internUser(User user) {
        for (User u : userPool) {
            if (u == user)
                return u;
        }
        userPool.add(user);
        return user;
    }

    protected class PooledVideo {
        private Video video;
        private ArrayList<Annotation> annotations;

        public PooledVideo(int annotationCount) {
            video = new Video();
            reserveAnnotations(annotationCount);
        }

        private void reserveAnnotations(int count) {

            annotations.ensureCapacity(count);
            while (annotations.size() < count) {
                annotations.add(new Annotation());
            }
        }

        Video create(int annotationCount) {

            reserveAnnotations(annotationCount);
            List<Annotation> subAnn = annotations.subList(annotationCount);
            video.setAnnotations(subAnn);
            return video;
        }
    };

    public OptimizedVideo(Video video) {

        List<Annotation> annotations = video.getAnnotations();
        int annotationCount = annotations.size();

        repository = video.getRepository();
        videoUri = video.getVideoUri();
        thumbUri = video.getThumbUri();
        id = video.getId();
        title = video.getTitle();
        genre = video.getGenre();
        tag = video.getTag();
        dateInMs = video.getDate().getTime();
        author = internUser(video.getAuthor());
        location = video.getLocation();

        annotationTime = new long[annotationCount];
        annotationX = new float[annotationCount];
        annotationY = new float[annotationCount];
        annotationText = new String[annotationCount];
        annotationAuthor = new User[annotationCount];

        for (int i = 0; i < annotationCount; i++) {
            annotationTime[i] = annotations[i].getTime();
            annotationX[i] = annotations[i].getPosition().x;
            annotationY[i] = annotations[i].getPosition().y;
            annotationText[i] = annotations[i].getText();
            annotationAuthor[i] = internAuthor(annotations[i].getAuthor());
        }
    }

    Video inflate(PooledVideo pooled) {

        int annotationCount = annotationTime.length;
        Video video = pooled.create(annotationCount);

        video.setRepository(repository);
        video.setVideoUri(new Uri(videoUri));
        video.setThumbUri(new Uri(thumbUri));
        video.setId(id);
        video.setTitle(title);
        video.setGenre(genre);
        video.setTag(tag);
        video.setDate(new Date(dateInMs));
        video.setAuthor(author);
        video.setLocation(location);

        for (int i = 0; i < annotationCount; i++) {
            video.annotations[i].time = annotationTime[i];
            video.annotations[i].getPosition().set(annotationX[i], annotationY[i]);
            video.annotations[i].setText(annotationText[i]);
            video.annotations[i].setAuthor(annotationText[i]);
        }

        return video;
    }
}

