package org.digma.intellij.plugin.jcef.common;

import com.intellij.util.messages.Topic;

public interface UserRegistrationEvent {


    @Topic.AppLevel
    Topic<UserRegistrationEvent> USER_REGISTRATION_TOPIC = Topic.create("USER_REGISTRATION_EVENT", UserRegistrationEvent.class);


    void userRegistered(String email);


}
