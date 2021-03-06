package com.zzw.service;

import com.zzw.Entity.*;
import com.zzw.dao.videoDao;
import com.zzw.exception.conditionException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class videoService {

    @Autowired
    private com.zzw.dao.videoDao videoDao;

    @Autowired
    private UserCoinService userCoinService;

    @Autowired
    private userService userService;


    public void addVideos(Video video) {
        Date now = new Date();

        video.setCreateTime(new Date());

        videoDao.addVideos(video);

//        Long videoId = video.getId();
//
//        List<VideoTag> tagList = video.getVideoTagList();
//
//        if(tagList==null){
//            tagList = new ArrayList<>();
//        }
//
//        tagList.forEach(item ->{
//            item.setCreateTime(now);
//            item.setVideoId(videoId);
//        });
//
//        videoDao.batchAddVideoTag(tagList);
    }

    public PageResult<Video> pageListVideos(Integer size, Integer no, String area) {

        if(size==null||no==null) throw new conditionException("???????????????");

        Map<String,Object> params = new HashMap<>();

        params.put("start",(no-1)*size);
        params.put("limit",size);
        params.put("area",area);

        List<Video> list = new ArrayList<>();
        Integer total =  videoDao.pageCountVideos(params);

        if(total>0){

            list = videoDao.pageListVideos(params);

        }

        return new PageResult<>(total,list);


    }
    //???????????????
    public void addVideoLike(Long videoId, Long userId) {
       Video video =  videoDao.getVideoById(videoId);
       if(video == null) throw new conditionException("????????????!");
       VideoLike videoLike =  videoDao.getVideoLikeByVideoId(videoId,userId);
       if(videoLike!=null)throw new conditionException("????????????????????????!");
        videoLike  = new VideoLike();
        videoLike.setUserId(userId);
        videoLike.setVideoId(videoId);
        videoLike.setCreateTime(new Date());
        videoDao.addVideoLike(videoLike);
    }

    public void deleteVideoLike(Long videoId, Long userId) {
        videoDao.deleteVideoLike(videoId,userId);
    }

    public Map<String, Object> getVideoLike(Long videoId, Long userId) {
        Long count  =videoDao.getVideoLikes(videoId);
        VideoLike videoLike = videoDao.getVideoLikeByVideoId(videoId,userId);//??????????????????????????????????????????
        Boolean flag = videoLike !=null;
        Map<String, Object> res = new HashMap<>();
        res.put("count",count);
        res.put("like",flag);
        return res;
    }
    @Transactional
    public void addVideoCollections(VideoCollection videoCollection, Long userId) {
        Long videoId = videoCollection.getVideoId();
        Long groupId = videoCollection.getGroupId();
        if(videoId == null||groupId==null) throw new conditionException("???????????????");
        Video video = videoDao.getVideoById(videoId);
        if(video==null)  throw new conditionException("????????????!");

        //??????????????????????????????
        videoDao.deleteVideoColletion(videoId,userId);
        //???????????????????????????
        videoCollection.setCreateTime(new Date());
        videoCollection.setUserId(userId);
        videoDao.addVideoColletion(videoCollection);
    }

    public void deleteVideoCollections(Long videoId, Long userId) {
        videoDao.deleteVideoColletion(videoId,userId);
    }

    public Map<String, Object> getVideoCollections(Long videoId, Long userId) {
        Long count  =videoDao.getVideoColletion(videoId);
        VideoCollection VideoCollection = videoDao.getVideoColletionByVideoIdAndUserId(videoId, userId);//??????????????????????????????????????????
        Boolean flag = VideoCollection !=null;
        Map<String, Object> res = new HashMap<>();
        res.put("count",count);
        res.put("like",flag);
        return res;
    }

    //????????????????????????
    public void addVideoCoins(VideoCoin videoCoin, Long userId) {
        Long videoId = videoCoin.getVideoId();
        Long amount = videoCoin.getAmount();
        if(videoId == null) throw new conditionException("???????????????");

        Video video = videoDao.getVideoById(videoId);
        if(video==null)  throw new conditionException("????????????!");

        Long userCoinsAmount =userCoinService.getUserCoinsAmount(userId);//````

        userCoinsAmount = userCoinsAmount==null?0:userCoinsAmount;

        if(amount>userCoinsAmount) throw new conditionException("??????????????????!");

        //???????????????????????????????????????
        VideoCoin dbVideoCoins = videoDao.getVideoCoinsByVideoIdAndUserId(videoId,userId);

        if(dbVideoCoins==null){//??????????????????-??????
            videoCoin.setCreateTime(new Date());
            videoCoin.setUserId(userId);
            videoDao.addVideoCoins(videoCoin);
        }else{//???????????????-??????

            Long dbAmount = dbVideoCoins.getAmount();
            dbAmount+=amount;
            //????????????
            videoCoin.setUserId(userId);
            videoCoin.setAmount(dbAmount);
            videoCoin.setUpdateTime(new Date());
            videoDao.updateVideoCoin(videoCoin);//??????
        }
        //??????????????????????????? ????????????????????? ??????????????????
        userCoinService.updateUserCoinsAmount(userId,(userCoinsAmount-amount));

    }

    //????????????
    public Map<String, Object> getVideoCoins(Long videoId, Long userId) {
        Long count  =videoDao.getVideoCoins(videoId);
        VideoCoin videoCoin = videoDao.getVideoCoinsByVideoIdAndUserId(videoId, userId);//??????????????????????????????????????????
        Boolean flag = videoCoin !=null;
        Map<String, Object> res = new HashMap<>();
        res.put("count",count);
        res.put("like",flag);
        return res;
    }


    //????????????
    public void addVideosComments(VideoComment videoComment, Long userId) {

        Long videoId = videoComment.getVideoId();

        if(videoId==null) throw new conditionException("????????????!");

        Video video = videoDao.getVideoById(videoId);

        if(video==null)throw new conditionException("???????????????!");

        videoComment.setUserId(userId);
        videoComment.setCreateTime(new Date());


        //????????????????????????
        videoDao.addVideosComments(videoComment);




    }


    //????????????-????????????
    public PageResult<VideoComment> pageListVideosComments(Integer size, Integer no, Long videoId) {
        Video video = videoDao.getVideoById(videoId);

        if(video==null) throw new conditionException("???????????????");

        Map<String,Object> params = new HashMap<>();
        params.put("start",(no-1)*size);
        params.put("limit",size);
        params.put("videoId",videoId);
        Integer total =  videoDao.pageCountVideosComents(params);
        List<VideoComment> list = new ArrayList<>();

        if(total>0){
            list = videoDao.pageListVideosComments(params);
            //????????????????????????
            List<Long> parentList = list.stream().map(VideoComment::getId).collect(Collectors.toList());
            List<VideoComment> childCommentList = videoDao.batchGetVideoCommentsByRootId(parentList);
            //????????????????????????
            Set<Long> userIdSet = list.stream().map(VideoComment::getUserId).collect(Collectors.toSet());
            Set<Long> replyUserIdSet = childCommentList.stream().map(VideoComment::getUserId).collect(Collectors.toSet());
            userIdSet.addAll(replyUserIdSet);
            List<UserInfo> userInfoList =  userService.batchGetUserInfoByUserIds(userIdSet);
            Map<Long,UserInfo> userInfoMap =userInfoList.stream().collect(Collectors.toMap(UserInfo::getUserId,userInfo -> userInfo));
            list.forEach(comment->{
                Long id = comment.getId();
                ArrayList<VideoComment> childList = new ArrayList<>();
                childCommentList.forEach(child->{
                    if(id.equals(child.getRootId())){
                        child.setUserInfo(userInfoMap.get(child.getUserId()));
                        child.setReplyUserInfo(userInfoMap.get(child.getReplyUserInfo()));
                        childList.add(child);
                    }
                });
                comment.setChildList(childList);
                comment.setUserInfo(userInfoMap.get(comment.getUserId()));
            });

        }

        return new PageResult<VideoComment>(total,list);
    }

    public Map<String, Object> getVideoDetails(Long videoId) {
        Video video =  videoDao.getVideoDetails(videoId);
        Long userId = video.getUserId();
        User user = userService.getUserInfo(userId);
        UserInfo userInfo = user.getUserInfo();//?????????????????????
        HashMap<String, Object> res = new HashMap<>();
        res.put("video",video);
        res.put("userInfo",userInfo);
        return res;
    }

    //????????????
    public List<Video> recommend(Long userId) throws TasteException {

        List<userPreference> list=  videoDao.getAllUserPreference();//???????????????????????????

        DataModel dataModel = this.creatDataModel(list);//???????????????????????? ?????? ????????????
        //?????????????????????
        UncenteredCosineSimilarity similarity = new UncenteredCosineSimilarity(dataModel);//???????????????????????????
                System.out.println(similarity.userSimilarity(11,12));//?????? 11 ????????? 12 ????????????????????????
        //??????????????????
        NearestNUserNeighborhood userNeighborhood = new NearestNUserNeighborhood(2, similarity,dataModel);//??????????????????????????????
        long[] ar = userNeighborhood.getUserNeighborhood(userId);

        //???????????????
        Recommender recommender = new GenericUserBasedRecommender(dataModel,userNeighborhood,similarity);//????????????

        //????????????
        List<RecommendedItem> recommendedItems = recommender.recommend(userId, 5);

        List<Long> itemIds = recommendedItems.stream().map(RecommendedItem::getItemID).collect(Collectors.toList());

         List<Video> rem = videoDao.batchGetVideoByIds(itemIds); //???????????? ?????????????????? ?????? ??????id

        return rem;
    }

    private DataModel creatDataModel(List<userPreference> userPreferenceList) {

        FastByIDMap<PreferenceArray> fastByIDMap = new FastByIDMap<>();

        Map<Long, List<userPreference>> map = userPreferenceList.stream().collect(Collectors.groupingBy(userPreference::getUserId));

        Collection<List<userPreference>> list = map.values();

        for(List<userPreference> userPreferences:list){

            GenericPreference[] array= new GenericPreference[userPreferences.size()];

            for(int i=0;i<userPreferences.size();++i){
                userPreference userPreference = userPreferences.get(i);
                GenericPreference item = new GenericPreference(userPreference.getUserId(),userPreference.getVideoId(),userPreference.getValue());
                array[i] = item;
            }

            fastByIDMap.put(array[0].getUserID(),new GenericUserPreferenceArray(Arrays.asList(array)));

        }

        return new GenericDataModel(fastByIDMap);

    }
}
