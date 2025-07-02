package com.zegocloud.zimkit.common;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;


import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService;
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig;
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoTranslationText;
import com.zegocloud.zimkit.common.ZIMKitRouter;
import com.zegocloud.zimkit.common.ZIMKitConstant;
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType;

public class CustomZIMKitRouter extends ZIMKitRouter {

    private static final long APP_ID = 60318532L;
    private static final String APP_SIGN = "ffc7aa3de9a04b6342e35a315ae28fbc974546c05c48ab22085cc79364484eb8";

    /**
     * Enhanced version with call initialization
     */
    public static void toMessageActivityWithCall(
            Context context,
            String conversationId,
            String currentUserId,
            String currentUserName,
            ZIMKitConversationType type
    ) {
        // Initialize ZegoUIKit call service
        initCallService(context, currentUserId, currentUserName);

        // Call original router method
        if (type == ZIMKitConversationType.ZIMKitConversationTypePeer) {
            toMessageActivity(context, conversationId, type);
        } else {
            toMessageActivity(context, conversationId, "Group Chat", null, type);
        }
    }

    /**
     * Enhanced version with call initialization and user details
     */
    public static void toMessageActivityWithCall(
            Context context,
            String conversationId,
            String name,
            String avatar,
            String currentUserId,
            String currentUserName,
            ZIMKitConversationType type
    ) {
        // Initialize ZegoUIKit call service
        initCallService(context, currentUserId, currentUserName);

        // Call original router method
        toMessageActivity(context, conversationId, name, avatar, type);
    }

    private static boolean isCallServiceInitialized = false;

    private static void initCallService(Context context, String userId, String userName) {
        if (isCallServiceInitialized) return;

        Application application = (Application) context.getApplicationContext();

        ZegoUIKitPrebuiltCallInvitationConfig config = new ZegoUIKitPrebuiltCallInvitationConfig();

        config.translationText = new ZegoTranslationText(); // Prevents null crash
        // Optional: Add translations like
        // config.translationText.incomingCallPageTitle = "Incoming Call";

        ZegoUIKitPrebuiltCallService.init(
                application,
                APP_ID,
                APP_SIGN,
                userId,
                userName,
                config
        );

        Log.d("ZegoInit", "Zego call service initialized for user: " + userId);

        isCallServiceInitialized = true;  // Mark it as initialized
    }


}