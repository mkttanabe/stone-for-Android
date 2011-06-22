/*
 * Copyright (C) 2011 KLab Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package jp.klab.stone;

import jp.klab.stone.IStoneCallback;

interface IStoneService {
	int __StoneProcessIsRunning();
	int __ExecuteStoneProcess(String param);
	int __TerminateStoneProcess();
	void __ClearStoneLogCache();
	void __registerCallback(IStoneCallback cb);
	void __unregisterCallback(IStoneCallback cb);
}
