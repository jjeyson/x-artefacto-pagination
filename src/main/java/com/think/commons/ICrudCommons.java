package com.think.commons;

public interface ICrudCommons<REQ, RES, ID> {
    RES save(REQ request);

    RES update(ID id, REQ request);

    RES findById(ID id);

    RES delete(ID id);
}
