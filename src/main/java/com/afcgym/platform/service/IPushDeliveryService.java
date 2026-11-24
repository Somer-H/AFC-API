package com.afcgym.platform.service;

import com.afcgym.platform.service.dto.PushDeliveryReport;
import com.afcgym.platform.service.dto.PushMessage;
import java.util.List;

public interface IPushDeliveryService {

    PushDeliveryReport deliver(List<PushMessage> messages);
}
