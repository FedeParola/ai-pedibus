import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RxStompService } from '@stomp/ng2-stompjs';
import { Message } from '@stomp/stompjs';
import { Subject } from 'rxjs';

import { environment } from '../environments/environment'


@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsUpdate$: Subject<number> = new Subject<number>();

  constructor(private http: HttpClient,
              private rxStompService: RxStompService) {
    
    this.rxStompService.watch('/user/topic/notifications').subscribe(
      (message: Message) => {
        this.notificationsUpdate$.next(+message.body);
      }
    );
  }

  /**
   * Return an observable that emits the number of pending notifications every time there is
   * an update in the notifications set.
   */
  getNotificationsUpdate$() {
    return this.notificationsUpdate$.asObservable();
  }

  getNotifications(username: string, page: number, size: number){
    return this.http.get(environment.apiUrl+'/users/'+username+'/notifications?page='+page+'&size='+size);
  }

  removeNotification(notificationId){
    return this.http.delete(environment.apiUrl+'/notifications/'+notificationId);
  }

  updateNotification(notificationId, read: boolean){
    return this.http.put(environment.apiUrl+'/notifications/'+notificationId, {read});
  }
}
