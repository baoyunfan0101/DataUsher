package com.datausher.data.quality.core;

import com.datausher.data.quality.api.ProfileJob;
import com.datausher.data.quality.api.ProfileJobId;
import com.datausher.platform.shared.context.RequestContext;

import java.util.List;

public interface ProfileWorker {
    List<ProfileJob> findPending(int limit);

    ProfileJob dispatch(ProfileJobId jobId, RequestContext requestContext);
}
