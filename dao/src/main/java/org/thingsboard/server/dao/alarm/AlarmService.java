/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.page.TimePageData;

import java.util.Optional;

/**
 * Created by ashvayka on 11.05.17.
 */
public interface AlarmService {

    Optional<Alarm> saveIfNotExists(Alarm alarm);

    ListenableFuture<Boolean> updateAlarm(Alarm alarm);

    ListenableFuture<Boolean> ackAlarm(Alarm alarm);

    ListenableFuture<Boolean> clearAlarm(AlarmId alarmId);

    ListenableFuture<TimePageData<Alarm>> findAlarms(AlarmQuery query);

}
