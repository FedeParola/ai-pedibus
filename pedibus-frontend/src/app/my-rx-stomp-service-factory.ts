import { InjectableRxStompConfig, RxStompService } from '@stomp/ng2-stompjs';


/**
 * Unlike rxStompServiceFactory provided by ng2-stompjs library this method cretes
 * a RxStompService that is configured but not activated.
 */
export function myRxStompServiceFactory(rxStompConfig: InjectableRxStompConfig): RxStompService {
  const rxStompService = new RxStompService();
  
  rxStompService.configure(rxStompConfig);

  return rxStompService;
}