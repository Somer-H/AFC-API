package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.CheckInEntity;

public interface ICheckInService {

    CheckInEntity checkIn(Long memberId, String pin, String fingerprintToken);
}
